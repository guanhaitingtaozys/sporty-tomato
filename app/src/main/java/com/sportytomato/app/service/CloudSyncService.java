package com.sportytomato.app.service;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;
import com.sportytomato.app.model.database.AppDatabase;
import com.sportytomato.app.model.entity.*;
import com.sportytomato.app.repository.UserRepository;
import com.sportytomato.app.repository.emas.EMASUserRepository;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class CloudSyncService {
    private static final String TAG = "CloudSyncService";
    private final Executor executor = Executors.newSingleThreadExecutor();
    private static CloudSyncService instance;
    private AppDatabase database;
    private EMASClient emasClient;
    private Context appContext;

    private CloudSyncService() {}
    public static synchronized CloudSyncService getInstance() { if (instance == null) instance = new CloudSyncService(); return instance; }
    public void init(AppDatabase database, Context context) { this.database = database; this.appContext = context.getApplicationContext(); this.emasClient = EMASClient.getInstance(); }

    public void syncUserToCloud(User user) {
        if (!emasClient.isInitialized() || user == null) return;
        executor.execute(() -> doSyncUser(user));
    }

    private boolean doSyncUser(User user) {
        try {
            if (user.getShortId() == null || user.getShortId().isEmpty()) return false;
            JSONObject data = new JSONObject();
            data.put("userId", user.getUserId()); data.put("nickname", user.getNickname()); data.put("shortId", user.getShortId());
            data.put("phone", user.getPhone() != null ? user.getPhone() : "");
            data.put("passwordHash", user.getPasswordHash() != null ? user.getPasswordHash() : "");
            data.put("avatarUrl", user.getAvatarUrl() != null ? user.getAvatarUrl() : "");
            data.put("createdAt", user.getCreatedAt()); data.put("updatedAt", user.getUpdatedAt());

            JSONObject query = new JSONObject(); query.put("shortId", user.getShortId());
            String findParams = EMASClient.buildDbParams("findOne", "users", null, query.toString());
            String findResultStr = emasClient.callFunction("sporty-api", findParams);
            boolean exists = false;
            if (findResultStr != null) { try { JSONObject findResult = new JSONObject(findResultStr); exists = findResult.optJSONObject("result") != null; } catch (Exception e) {} }

            if (exists) {
                JSONObject updateData = new JSONObject(); updateData.put("$set", data);
                String updateParams = EMASClient.buildDbParams("updateOne", "users", updateData.toString(), query.toString());
                emasClient.callFunction("sporty-api", updateParams);
            } else {
                String insertParams = EMASClient.buildDbParams("insertOne", "users", data.toString(), null);
                emasClient.callFunction("sporty-api", insertParams);
            }
            return true;
        } catch (Exception e) { Log.e(TAG, "doSyncUser error: " + e.getMessage(), e); return false; }
    }

    public void deleteUserFromCloud(String shortId, int userId, DeleteCallback callback) {
        if (!emasClient.isInitialized()) { if (callback != null) callback.onComplete(false, "网络不可用"); return; }
        executor.execute(() -> {
            try {
                String q = "{\"shortId\":\"" + shortId + "\"}";
                String params = EMASClient.buildDbParams("deleteOne", "users", null, q);
                emasClient.callFunction("sporty-api", params);
                if (callback != null) callback.onComplete(true, "云端数据已清除");
            } catch (Exception e) { if (callback != null) callback.onComplete(false, "删除失败: " + e.getMessage()); }
        });
    }

    public void downloadFriendProfiles() {
        if (!emasClient.isInitialized()) return;
        executor.execute(() -> {
            try {
                UserRepository userRepo = RepositoryManager.getInstance().getUserRepo();
                User currentUser = userRepo.getCurrentUser();
                if (currentUser == null) return;
                List<Friendship> friendships = database.friendDao().getAcceptedFriendships(currentUser.getUserId());
                if (friendships == null || friendships.isEmpty()) return;
                EMASUserRepository emasUserRepo = EMASUserRepository.getInstance();
                for (Friendship f : friendships) {
                    int friendUserId = (f.getUserId() == currentUser.getUserId()) ? f.getFriendUserId() : f.getUserId();
                    User localFriend = database.userDao().getUserById(friendUserId);
                    if (localFriend == null || localFriend.getShortId() == null) continue;
                    User cloudUser = emasUserRepo.findByShortId(localFriend.getShortId());
                    if (cloudUser != null) userRepo.ensureLocalUser(cloudUser);
                }
            } catch (Exception e) { Log.e(TAG, "downloadFriendProfiles error: " + e.getMessage()); }
        });
    }

    public void syncAllDataToCloud(int userId, SyncCallback callback) { /* full sync logic */ }

    public interface SyncCallback { void onComplete(boolean success, String message); }
    public interface DeleteCallback { void onComplete(boolean success, String message); }
}