package com.sportytomato.app.view;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Shader;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import com.sportytomato.app.databinding.FragmentFriendsBinding;
import com.sportytomato.app.model.database.AppDatabase;
import com.sportytomato.app.model.entity.Friendship;
import com.sportytomato.app.model.entity.Message;
import com.sportytomato.app.model.entity.User;
import com.sportytomato.app.repository.FriendRepository;
import com.sportytomato.app.repository.MessageRepository;
import com.sportytomato.app.repository.UserRepository;
import com.sportytomato.app.model.entity.TomatoState;
import com.sportytomato.app.repository.TomatoRepository;
import com.sportytomato.app.repository.emas.EMASUserRepository;
import com.sportytomato.app.service.CloudSyncService;
import com.sportytomato.app.service.EMASClient;
import com.sportytomato.app.service.FriendService;
import com.sportytomato.app.service.RepositoryManager;
import com.sportytomato.app.service.AchievementService;
import com.sportytomato.app.util.DialogHelper;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 好友页 Fragment — Sprint 3 重写
 */
public class FriendsFragment extends Fragment {

    private FragmentFriendsBinding binding;
    private FriendService friendService;
    private FriendRepository friendRepo;
    private UserRepository userRepo;
    private MessageRepository messageRepo;
    private volatile User currentUser;
    private boolean showingRequests = false;
    private volatile boolean isSyncing = false;
    private final Object syncLock = new Object();
    private volatile long lastCloudSyncTime = 0;

    public static FriendsFragment newInstance() {
        FriendsFragment fragment = new FriendsFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentFriendsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        RepositoryManager rm = RepositoryManager.getInstance();
        friendRepo = rm.getFriendRepo();
        userRepo = rm.getUserRepo();
        messageRepo = rm.getMessageRepo();
        friendService = new FriendService(friendRepo, userRepo);

        setupTabs();
        setupSearch();
        setupRankButton();
        setupFab();
        setupSwipeRefresh();

        // 后台加载当前用户后刷新内容
        new Thread(() -> {
            currentUser = userRepo.getCurrentUser();
            if (currentUser != null) {
                new Handler(Looper.getMainLooper()).post(() -> {
                    if (binding != null) refreshContent();
                });
            }
        }).start();
    }

    private void setupTabs() {
        binding.tvTabFriends.setOnClickListener(v -> {
            showingRequests = false;
            updateTabStyles();
            refreshContent();
        });

        binding.tvTabRequests.setOnClickListener(v -> {
            showingRequests = true;
            updateTabStyles();
            refreshContent();
        });
    }

    private void updateTabStyles() {
        if (showingRequests) {
            binding.tvTabFriends.setTextColor(requireContext().getResources().getColor(
                    com.sportytomato.app.R.color.on_surface_variant));
            binding.tvTabFriends.setTypeface(null, android.graphics.Typeface.NORMAL);
            binding.tvTabRequests.setTextColor(requireContext().getResources().getColor(
                    com.sportytomato.app.R.color.primary));
            binding.tvTabRequests.setTypeface(null, android.graphics.Typeface.BOLD);
        } else {
            binding.tvTabFriends.setTextColor(requireContext().getResources().getColor(
                    com.sportytomato.app.R.color.primary));
            binding.tvTabFriends.setTypeface(null, android.graphics.Typeface.BOLD);
            binding.tvTabRequests.setTextColor(requireContext().getResources().getColor(
                    com.sportytomato.app.R.color.on_surface_variant));
            binding.tvTabRequests.setTypeface(null, android.graphics.Typeface.NORMAL);
        }
    }

    private void setupSearch() {
        // 实时昵称过滤（在好友列表页输入文字时即时过滤）
        binding.etSearchShortId.addTextChangedListener(new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(android.text.Editable s) {
                if (!showingRequests && binding != null && currentUser != null) {
                    String keyword = s.toString().trim();
                    if (keyword.isEmpty()) {
                        refreshContent(); // 清空输入→显示完整列表
                    } else {
                        showFilteredFriendList(keyword);
                    }
                }
            }
        });

        binding.btnSearch.setOnClickListener(v -> {
            String input = binding.etSearchShortId.getText().toString().trim();
            if (input.isEmpty()) {
                Toast.makeText(requireContext(), "请输入好友ID或手机号", Toast.LENGTH_SHORT).show();
                return;
            }

            // 清除 TextWatcher 过滤显示的已有好友卡片，避免与搜索"未找到"Toast矛盾
            binding.llContent.removeAllViews();

            // 后台查询，避免主线程 Room 操作
            final String searchInput = input;
            new Thread(() -> {
                User target = findUserLocally(searchInput);

                if (target != null) {
                    new Handler(Looper.getMainLooper()).post(() -> {
                        if (binding == null || currentUser == null) return;
                        if (target.getUserId() == currentUser.getUserId()) {
                            Toast.makeText(requireContext(), "这是你自己", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        if (friendRepo.areFriends(currentUser.getUserId(), target.getUserId())) {
                            Toast.makeText(requireContext(), "已是好友", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        Intent intent = new Intent(requireContext(), AddFriendActivity.class);
                        intent.putExtra("target_user_id", target.getUserId());
                        startActivity(intent);
                    });
                    return;
                }

                // 本地未找到，尝试 EMAS 云端搜索
                if (!EMASClient.getInstance().isInitialized()) {
                    new Handler(Looper.getMainLooper()).post(() ->
                        Toast.makeText(requireContext(), "未找到该用户（已搜索本地和云端）", Toast.LENGTH_SHORT).show());
                    return;
                }

                new Handler(Looper.getMainLooper()).post(() ->
                    Toast.makeText(requireContext(), "正在云端搜索...", Toast.LENGTH_SHORT).show());

                EMASUserRepository emasUserRepo = EMASUserRepository.getInstance();
                User cloudTarget = emasUserRepo.findByShortId(searchInput);
                if (cloudTarget == null && searchInput.matches("^1\\d{10}$")) {
                    cloudTarget = emasUserRepo.findByPhone(searchInput);
                }
                if (cloudTarget == null) {
                    cloudTarget = emasUserRepo.findByShortId(searchInput);
                }
                if (cloudTarget == null) {
                    cloudTarget = emasUserRepo.findByPhone(searchInput);
                }
                final User finalTarget = cloudTarget;
                requireActivity().runOnUiThread(() -> {
                    if (finalTarget == null) {
                        Toast.makeText(requireContext(),
                                "未找到该用户（已搜索本地和云端）", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    User synced = userRepo.ensureLocalUser(finalTarget);
                    if (synced == null) {
                        Toast.makeText(requireContext(),
                                "同步用户信息失败", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (currentUser != null && synced.getUserId() == currentUser.getUserId()) {
                        Toast.makeText(requireContext(), "这是你自己", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (currentUser != null && friendRepo.areFriends(currentUser.getUserId(), synced.getUserId())) {
                        Toast.makeText(requireContext(), "已是好友", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    Intent intent = new Intent(requireContext(), AddFriendActivity.class);
                    intent.putExtra("target_user_id", synced.getUserId());
                    startActivity(intent);
                });
            }).start();
        });
    }

    private User findUserLocally(String input) {
        User target = null;
        if (input.matches("^\\d{6}$")) {
            target = userRepo.findByShortId(input);
        } else if (input.matches("^1\\d{10}$")) {
            target = userRepo.findByPhone(input);
        }
        if (target == null) {
            target = userRepo.findByShortId(input);
        }
        if (target == null) {
            target = userRepo.findByPhone(input);
        }
        return target;
    }

    private void setupRankButton() {
        binding.btnRank.setOnClickListener(v -> {
            startActivity(new Intent(requireContext(), RankActivity.class));
        });
    }

    private void setupFab() {
        binding.fabAddFriend.setOnClickListener(v -> {
            // FAB 直接跳转添加好友页面（搜索模式）
            startActivity(new Intent(requireContext(), AddFriendActivity.class));
        });
    }

    private void setupSwipeRefresh() {
        binding.swipeRefresh.setOnRefreshListener(() -> {
            if (showingRequests) {
                syncCloudManually();
            } else {
                syncFriendsManually();
            }
            // 延迟关闭刷新动画，给后台请求一点时间
            binding.swipeRefresh.postDelayed(() -> {
                if (binding != null) binding.swipeRefresh.setRefreshing(false);
            }, 1500);
        });
    }

    private void refreshContent() {
        if (currentUser == null || friendRepo == null || binding == null) return;
        final int userId = currentUser.getUserId();

        // 不再自动从云端拉取，改为用户下拉或点击按钮时手动触发

        final boolean isRequests = showingRequests;
        final LinearLayout content = binding.llContent;
        content.removeAllViews();

        new Thread(() -> {
            int pendingCount = friendRepo.getPendingCount(userId);
            new Handler(Looper.getMainLooper()).post(() -> {
                if (binding == null) return;
                if (pendingCount > 0) {
                    binding.tvBadge.setVisibility(View.VISIBLE);
                    binding.tvBadge.setText(String.valueOf(pendingCount));
                } else {
                    binding.tvBadge.setVisibility(View.GONE);
                }

                LinearLayout c = binding.llContent;
                c.removeAllViews();

                if (isRequests) {
                    showPendingRequests(c);
                } else {
                    showFriendList(c);
                }
            });
        }).start();
    }

    /**
     * 从 EMAS 云端拉取发给当前用户的待处理好友申请，
     * 同步到本地 Room（在后台线程执行，完成后刷新UI）
     */
    private void syncCloudPendingRequests() {
        if (!EMASClient.getInstance().isInitialized() || currentUser == null) return;
        final String myShortId = currentUser.getShortId();
        if (myShortId == null || myShortId.isEmpty()) return;

        synchronized (syncLock) {
            if (isSyncing) return;
            isSyncing = true;
        }
        new Thread(() -> {
            try {
                JSONObject query = new JSONObject();
                query.put("toShortId", myShortId);
                query.put("status", 0);

                String params = EMASClient.buildDbParamsWithOptions("find", "friendships",
                        null, query.toString(), null);
                String result = EMASClient.getInstance().callFunction("sporty-api", params);

                if (result == null) return;

                JSONObject json = new JSONObject(result);
                if (!json.optBoolean("success")) return;

                JSONArray arr = json.optJSONArray("result");
                if (arr == null || arr.length() == 0) return;

                int syncedCount = 0;
                for (int i = 0; i < arr.length(); i++) {
                    JSONObject req = arr.getJSONObject(i);
                    String fromShortId = req.optString("fromShortId", "");

                    // 1. 按 shortId 查找发送方用户（先本地再云端）
                    User fromUser = userRepo.findByShortId(fromShortId);
                    if (fromUser == null) {
                        EMASUserRepository emasRepo = EMASUserRepository.getInstance();
                        fromUser = emasRepo.findByShortId(fromShortId);
                        if (fromUser != null) {
                            fromUser = userRepo.ensureLocalUser(fromUser);
                        }
                    }

                    if (fromUser == null) {
                        android.util.Log.w("FriendsFrag", "云端申请: 未找到发送方 shortId=" + fromShortId);
                        continue;
                    }

                    // 2. 检查本地是否已有记录（并修正方向）
                    Friendship existing = friendRepo.findFriendship(
                            fromUser.getUserId(), currentUser.getUserId());
                    if (existing != null) {
                        // getPendingRequests 查询条件是 friendUserId = currentUser
                        // 如果现有记录方向反了（userId=currentUser），删除并重建
                        if (existing.getFriendUserId() == currentUser.getUserId()
                                && existing.getStatus() == 0) {
                            continue; // 方向正确，跳过
                        }
                        // 方向反了：删除旧记录
                        friendRepo.deleteFriend(existing.getId());
                    }

                    // 3. 插入本地 Room
                    friendRepo.sendRequest(fromUser.getUserId(), currentUser.getUserId(),
                            req.optString("verifyMessage", ""));
                    syncedCount++;
                }

                if (syncedCount > 0) {
                    android.util.Log.i("FriendsFrag", "从云端同步了 " + syncedCount + " 条好友申请");
                }
            } catch (Exception e) {
                android.util.Log.e("FriendsFrag", "云端拉取申请异常: " + e.getMessage());
            } finally {
                isSyncing = false;
            }

            // 刷新 UI（主线程）- syncCloudPendingRequests
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    if (binding != null && currentUser != null) {
                        refreshContent();
                        Toast.makeText(requireContext(), "好友申请已刷新", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        }).start();
    }

    /**
     * 手动刷新云端申请，强制拉取一次
     */
    private void syncCloudManually() {
        synchronized (syncLock) {
            isSyncing = false;
        }
        syncCloudPendingRequests();
    }

    private void syncFriendsManually() {
        if (!EMASClient.getInstance().isInitialized() || currentUser == null) {
            Toast.makeText(requireContext(), "请先登录", Toast.LENGTH_SHORT).show();
            return;
        }
        syncFriendsFromCloud();
    }

    /**
     * 从 EMAS 云端拉取已接受的好友关系（status=1），同步到本地 Room
     */
    private void syncFriendsFromCloud() {
        syncFriendsFromCloud(false);
    }

    private void syncFriendsFromCloud(boolean silent) {
        if (!EMASClient.getInstance().isInitialized() || currentUser == null) return;
        final String myShortId = currentUser.getShortId();
        if (myShortId == null || myShortId.isEmpty()) return;

        synchronized (syncLock) {
            if (isSyncing) return;
            isSyncing = true;
        }
        lastCloudSyncTime = System.currentTimeMillis();
        new Thread(() -> {
            try {
                syncCloudFriendships("fromShortId", myShortId, 1);
                syncCloudFriendships("toShortId", myShortId, 1);
            } catch (Exception e) {
                android.util.Log.e("FriendsFrag", "云端好友同步异常: " + e.getMessage());
            } finally {
                isSyncing = false;
            }

            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    if (binding != null && currentUser != null) {
                        refreshContent();
                        if (!silent) {
                            Toast.makeText(requireContext(), "好友列表已刷新", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }

            // 下载好友资料（头像等），完成后刷新列表
            CloudSyncService.getInstance().downloadFriendProfiles();
            // 延迟刷新，等待头像下载完成（非阻塞方式）
            if (getActivity() != null && binding != null) {
                binding.getRoot().postDelayed(() -> {
                    if (binding != null && currentUser != null) {
                        refreshContent();
                    }
                }, 2000);
            }
        }).start();
    }

    private int syncCloudFriendships(String key, String shortId, int status) {
        int count = 0;
        try {
            JSONObject query = new JSONObject();
            query.put(key, shortId);
            query.put("status", status);

            String params = EMASClient.buildDbParamsWithOptions("find", "friendships",
                    null, query.toString(), null);
            String result = EMASClient.getInstance().callFunction("sporty-api", params);

            if (result == null) return 0;

            JSONObject json = new JSONObject(result);
            if (!json.optBoolean("success")) return 0;

            JSONArray arr = json.optJSONArray("result");
            if (arr == null || arr.length() == 0) return 0;

            for (int i = 0; i < arr.length(); i++) {
                JSONObject req = arr.getJSONObject(i);
                String fromShortId = req.optString("fromShortId", "");
                String toShortId = req.optString("toShortId", "");
                long cloudUpdatedAt = req.optLong("updatedAt", System.currentTimeMillis());

                User userA = findOrSyncUser(fromShortId);
                User userB = findOrSyncUser(toShortId);
                if (userA == null || userB == null) continue;

                Friendship existing = friendRepo.findFriendship(userA.getUserId(), userB.getUserId());
                if (existing != null && existing.getStatus() == status) continue;

                if (existing != null) {
                    if (status == 1) {
                        friendRepo.acceptRequestWithTime(existing.getId(), cloudUpdatedAt);
                        checkFriendAchievement();
                    } else if (status == 2) {
                        friendRepo.rejectRequest(existing.getId());
                    }
                } else {
                    friendRepo.sendRequest(userA.getUserId(), userB.getUserId(),
                            req.optString("verifyMessage", ""));
                    if (status == 1) {
                        Friendship f = friendRepo.findFriendship(userA.getUserId(), userB.getUserId());
                        if (f != null) {
                            friendRepo.acceptRequestWithTime(f.getId(), cloudUpdatedAt);
                            checkFriendAchievement();
                        }
                    }
                }
                count++;
            }
        } catch (Exception e) {
            android.util.Log.e("FriendsFrag", "同步好友关系异常: " + e.getMessage());
        }
        return count;
    }

    private User findOrSyncUser(String shortId) {
        User u = userRepo.findByShortId(shortId);
        if (u != null) return u;
        EMASUserRepository emasRepo = EMASUserRepository.getInstance();
        u = emasRepo.findByShortId(shortId);
        if (u != null) {
            u = userRepo.ensureLocalUser(u);
        }
        return u;
    }

    /**
     * 从 EMAS 云端拉取我发出去的申请的 status 变化（对方是否接受/拒绝），
     * 并同步到本地 Room
     */
    private void syncSentRequestsStatus() {
        if (!EMASClient.getInstance().isInitialized() || currentUser == null) return;
        final String myShortId = currentUser.getShortId();
        if (myShortId == null || myShortId.isEmpty()) return;

        new Thread(() -> {
            try {
                JSONObject query = new JSONObject();
                query.put("fromShortId", myShortId);

                String params = EMASClient.buildDbParamsWithOptions("find", "friendships",
                        null, query.toString(), null);
                String result = EMASClient.getInstance().callFunction("sporty-api", params);

                if (result == null) return;

                JSONObject json = new JSONObject(result);
                if (!json.optBoolean("success")) return;

                JSONArray arr = json.optJSONArray("result");
                if (arr == null || arr.length() == 0) return;

                int updated = 0;
                for (int i = 0; i < arr.length(); i++) {
                    JSONObject req = arr.getJSONObject(i);
                    String toShortId = req.optString("toShortId", "");
                    int cloudStatus = req.optInt("status", 0);

                    // 查找接收方用户（先本地再云端）
                    User toUser = userRepo.findByShortId(toShortId);
                    if (toUser == null) {
                        EMASUserRepository emasRepo = EMASUserRepository.getInstance();
                        toUser = emasRepo.findByShortId(toShortId);
                        if (toUser != null) {
                            toUser = userRepo.ensureLocalUser(toUser);
                        }
                    }
                    if (toUser == null) continue;

                    // 查找本地 friendship 记录
                    Friendship local = friendRepo.findFriendship(
                            currentUser.getUserId(), toUser.getUserId());
                    if (local == null) continue;

                    // 状态有变化才更新
                    if (local.getStatus() != cloudStatus) {
                        if (cloudStatus == 1) {
                            friendRepo.acceptRequest(local.getId());
                            checkFriendAchievement();
                        } else if (cloudStatus == 2) {
                            friendRepo.rejectRequest(local.getId());
                        }
                        updated++;
                    }
                }

                if (updated > 0) {
                    android.util.Log.i("FriendsFrag", "同步了 " + updated + " 条已发送申请的状态变化");
                }
            } catch (Exception e) {
                android.util.Log.e("FriendsFrag", "同步已发送申请状态异常: " + e.getMessage());
            }

            // 刷新 UI
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    if (binding != null && currentUser != null) {
                        refreshContent();
                    }
                });
            }
        }).start();
    }

    /**
     * 接受申请 → 同步到云端 (status: 0→1)
     */
    private void syncAcceptToCloud(String fromShortId, String toShortId) {
        if (!EMASClient.getInstance().isInitialized()) return;
        new Thread(() -> {
            try {
                JSONObject query = new JSONObject();
                query.put("fromShortId", fromShortId);
                query.put("toShortId", toShortId);
                query.put("status", 0);

                JSONObject data = new JSONObject();
                JSONObject set = new JSONObject();
                set.put("status", 1);
                set.put("updatedAt", System.currentTimeMillis());
                data.put("$set", set);

                String params = EMASClient.buildDbParams("updateOne", "friendships",
                        data.toString(), query.toString());
                EMASClient.getInstance().callFunction("sporty-api", params);
                android.util.Log.i("FriendsFrag", "云端已更新: " + fromShortId + "→" + toShortId + " status=1");
            } catch (Exception e) {
                android.util.Log.e("FriendsFrag", "云端更新失败: " + e.getMessage());
            }
        }).start();
    }

    /**
     * 拒绝申请 → 同步到云端 (status: 0→2)
     */
    private void syncRejectToCloud(String fromShortId, String toShortId) {
        if (!EMASClient.getInstance().isInitialized()) return;
        new Thread(() -> {
            try {
                JSONObject query = new JSONObject();
                query.put("fromShortId", fromShortId);
                query.put("toShortId", toShortId);
                query.put("status", 0);

                JSONObject data = new JSONObject();
                JSONObject set = new JSONObject();
                set.put("status", 2);
                set.put("updatedAt", System.currentTimeMillis());
                data.put("$set", set);

                String params = EMASClient.buildDbParams("updateOne", "friendships",
                        data.toString(), query.toString());
                EMASClient.getInstance().callFunction("sporty-api", params);
            } catch (Exception e) {
                android.util.Log.e("FriendsFrag", "云端更新失败: " + e.getMessage());
            }
        }).start();
    }

    private void showFriendList(LinearLayout content) {
        final LinearLayout finalContent = content;
        final int myUserId = currentUser.getUserId();

        // 后台线程批量查询好友关系和用户
        new Thread(() -> {
            List<Friendship> friendships = friendRepo.getAcceptedFriendships(currentUser.getUserId());
            if (friendships.isEmpty()) {
                new Handler(Looper.getMainLooper()).post(() -> {
                    if (binding == null) return;
                    addEmptyView(finalContent, "暂无好友，去搜索添加吧");
                });
                return;
            }

            // 收集所有好友ID
            List<Integer> friendIds = new ArrayList<>();
            Map<Integer, Friendship> idToFriendship = new HashMap<>();
            for (Friendship f : friendships) {
                int friendId = getOtherUserId(f);
                friendIds.add(friendId);
                idToFriendship.put(friendId, f);
            }

            List<User> users = AppDatabase.getInstance(requireContext()).userDao().getUsersByIds(friendIds);
            Map<Integer, User> userMap = new HashMap<>();
            for (User u : users) {
                userMap.put(u.getUserId(), u);
            }

            Map<Integer, Integer> unreadMap = new HashMap<>();
            if (messageRepo != null) {
                for (int friendId : friendIds) {
                    int unread = messageRepo.getUnreadCount(friendId, myUserId);
                    if (unread > 0) {
                        unreadMap.put(friendId, unread);
                    }
                }
            }

            new Handler(Looper.getMainLooper()).post(() -> {
                if (binding == null) return;
                finalContent.removeAllViews();
                for (Map.Entry<Integer, Friendship> entry : idToFriendship.entrySet()) {
                    int friendId = entry.getKey();
                    Friendship f = entry.getValue();
                    User friendUser = userMap.get(friendId);
                    if (friendUser == null) continue;
                    int unread = unreadMap.containsKey(friendId) ? unreadMap.get(friendId) : 0;
                    addFriendItem(finalContent, f, friendUser, friendId, unread);
                }
            });
        }).start();
    }

    private void showFilteredFriendList(String keyword) {
        final String lower = keyword.toLowerCase();
        final int myUserId = currentUser.getUserId();
        final LinearLayout finalContent = binding.llContent;
        finalContent.removeAllViews();

        // 后台线程批量查询并过滤
        new Thread(() -> {
            List<Friendship> friendships = friendRepo.getAcceptedFriendships(currentUser.getUserId());
            if (friendships.isEmpty()) {
                new Handler(Looper.getMainLooper()).post(() -> {
                    if (binding == null) return;
                    finalContent.removeAllViews();
                    addEmptyView(finalContent, "暂无好友");
                });
                return;
            }

            // 收集所有好友ID
            List<Integer> friendIds = new ArrayList<>();
            Map<Integer, Friendship> idToFriendship = new HashMap<>();
            for (Friendship f : friendships) {
                int friendId = getOtherUserId(f);
                friendIds.add(friendId);
                idToFriendship.put(friendId, f);
            }

            List<User> users = AppDatabase.getInstance(requireContext()).userDao().getUsersByIds(friendIds);

            // 过滤：匹配昵称
            List<Integer> filteredIds = new ArrayList<>();
            Map<Integer, User> userMap = new HashMap<>();
            for (User u : users) {
                userMap.put(u.getUserId(), u);
                if (u.getNickname() != null && u.getNickname().toLowerCase().contains(lower)) {
                    filteredIds.add(u.getUserId());
                }
            }

            if (filteredIds.isEmpty()) {
                new Handler(Looper.getMainLooper()).post(() -> {
                    if (binding == null) return;
                    finalContent.removeAllViews();
                    addEmptyView(finalContent, "没有匹配的好友");
                });
                return;
            }

            Map<Integer, Integer> unreadMap = new HashMap<>();
            if (messageRepo != null) {
                for (int friendId : filteredIds) {
                    int unread = messageRepo.getUnreadCount(friendId, myUserId);
                    if (unread > 0) {
                        unreadMap.put(friendId, unread);
                    }
                }
            }

            new Handler(Looper.getMainLooper()).post(() -> {
                if (binding == null) return;
                finalContent.removeAllViews();
                for (int friendId : filteredIds) {
                    Friendship f = idToFriendship.get(friendId);
                    User friendUser = userMap.get(friendId);
                    if (friendUser == null) continue;
                    int unread = unreadMap.containsKey(friendId) ? unreadMap.get(friendId) : 0;
                    addFriendItem(finalContent, f, friendUser, friendId, unread);
                }
            });
        }).start();
    }

    /**
     * 添加单个好友条目到内容区（复用逻辑）
     */
    private void addFriendItem(LinearLayout content, Friendship f, User friendUser, int friendId, int unread) {
        View item = LayoutInflater.from(requireContext()).inflate(
                com.sportytomato.app.R.layout.item_friend, content, false);

        ImageView ivAvatar = item.findViewById(com.sportytomato.app.R.id.iv_avatar);
        TextView tvNickname = item.findViewById(com.sportytomato.app.R.id.tv_nickname);
        TextView tvRemark = item.findViewById(com.sportytomato.app.R.id.tv_remark);
        TextView tvShortId = item.findViewById(com.sportytomato.app.R.id.tv_short_id);
        TextView tvLastActive = item.findViewById(com.sportytomato.app.R.id.tv_last_active);
        View dotOnlineStatus = item.findViewById(com.sportytomato.app.R.id.dot_online_status);
        TextView tvUnreadBadge = item.findViewById(com.sportytomato.app.R.id.tv_unread_badge);

        if (unread > 0) {
            tvUnreadBadge.setVisibility(View.VISIBLE);
            tvUnreadBadge.setText(unread > 99 ? "99+" : String.valueOf(unread));
        }

        long updatedAt = f.getUpdatedAt();
        dotOnlineStatus.setBackgroundResource(
                (updatedAt > 0 && (System.currentTimeMillis() - updatedAt) < 3600_000)
                        ? com.sportytomato.app.R.drawable.dot_online
                        : com.sportytomato.app.R.drawable.dot_offline);

        String avatarUrl = friendUser.getAvatarUrl();
        if (avatarUrl != null && !avatarUrl.isEmpty()) {
            try {
                Bitmap bmp = BitmapFactory.decodeFile(avatarUrl);
                if (bmp != null) ivAvatar.setImageBitmap(getCircularBitmap(bmp));
            } catch (Exception ignored) {}
        }

        tvNickname.setText(friendUser.getNickname());
        tvShortId.setText("ID:" + friendUser.getShortId());
        if (f.getRemarkName() != null && !f.getRemarkName().isEmpty()) {
            tvRemark.setVisibility(View.VISIBLE);
            tvRemark.setText(f.getRemarkName());
        }

        if (updatedAt > 0) {
            long now = System.currentTimeMillis();
            long diff = now - updatedAt;
            if (diff < 3600_000) tvLastActive.setText("最近活跃：1小时内");
            else if (diff < 86_400_000) tvLastActive.setText("最近活跃：" + (diff / 3600_000) + "小时前");
            else tvLastActive.setText("最近活跃：" + (diff / 86_400_000) + "天前");
        }

        item.setOnClickListener(vi -> showFriendMenu(f.getId(), friendUser));
        View btnMsg = item.findViewById(com.sportytomato.app.R.id.btn_message);
        btnMsg.setVisibility(View.VISIBLE);
        btnMsg.setOnClickListener(vi -> {
            Intent intent = new Intent(requireContext(), ChatActivity.class);
            intent.putExtra("friend_user_id", friendId);
            startActivity(intent);
        });
        content.addView(item);
    }

    private void showPendingRequests(LinearLayout content) {
        final LinearLayout finalContent = content;

        // 后台线程批量查询申请者
        new Thread(() -> {
            List<Friendship> requests = friendRepo.getPendingRequests(currentUser.getUserId());

            if (requests.isEmpty()) {
                new Handler(Looper.getMainLooper()).post(() -> {
                    if (binding == null) return;
                    addEmptyView(finalContent, "暂无好友申请，下拉刷新试试");
                });
                return;
            }

            // 收集所有申请者ID
            List<Integer> userIds = new ArrayList<>();
            for (Friendship req : requests) {
                userIds.add(req.getUserId());
            }

            final List<Friendship> finalRequests = requests;

            List<User> users = AppDatabase.getInstance(requireContext()).userDao().getUsersByIds(userIds);
            Map<Integer, User> userMap = new HashMap<>();
            for (User u : users) {
                userMap.put(u.getUserId(), u);
            }

            new Handler(Looper.getMainLooper()).post(() -> {
                if (binding == null) return;
                finalContent.removeAllViews();
                for (Friendship req : finalRequests) {
                    User fromUser = userMap.get(req.getUserId());
                    if (fromUser == null) continue;

                    View item = LayoutInflater.from(requireContext()).inflate(
                            com.sportytomato.app.R.layout.item_friend, finalContent, false);

                    ImageView ivAvatar = item.findViewById(com.sportytomato.app.R.id.iv_avatar);
                    TextView tvNickname = item.findViewById(com.sportytomato.app.R.id.tv_nickname);
                    TextView tvShortId = item.findViewById(com.sportytomato.app.R.id.tv_short_id);
                    TextView tvLastActive = item.findViewById(com.sportytomato.app.R.id.tv_last_active);

                    // 加载头像
                    String avatarUrl = fromUser.getAvatarUrl();
                    if (avatarUrl != null && !avatarUrl.isEmpty()) {
                        try {
                            Bitmap bmp = BitmapFactory.decodeFile(avatarUrl);
                            if (bmp != null) {
                                ivAvatar.setImageBitmap(getCircularBitmap(bmp));
                            }
                        } catch (Exception ignored) {}
                    }

                    tvNickname.setText(fromUser.getNickname());
                    tvShortId.setText("ID:" + fromUser.getShortId());
                    tvLastActive.setText("最近活跃：未知");

                    final User sender = fromUser;
                    item.setOnClickListener(vi -> {
                        DialogHelper.showStyledConfirmDialog(requireContext(),
                                "好友申请",
                                fromUser.getNickname() + " 申请加你为好友\n" +
                                        (req.getVerifyMessage() != null ? req.getVerifyMessage() : ""),
                                "接受", (d, w) -> {
                                    friendRepo.acceptRequest(req.getId());
                                    checkFriendAchievement();
                                    syncAcceptToCloud(sender.getShortId(), currentUser.getShortId());
                                    refreshContent();
                                },
                                "拒绝", (d, w) -> {
                                    friendRepo.rejectRequest(req.getId());
                                    syncRejectToCloud(sender.getShortId(), currentUser.getShortId());
                                    refreshContent();
                                });
                    });

                    finalContent.addView(item);
                }
            });
        }).start();
    }

    private void showFriendMenu(int friendshipId, User friendUser) {
        new com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
                .setTitle(friendUser.getNickname())
                .setItems(new String[]{"查看资料", "帮浇水", "偷能量", "删除好友"}, (d, which) -> {
                    switch (which) {
                        case 0:
                            // 查看好友资料（含头像 + 短ID + 关系建立时间）
                            showFriendProfileDialog(friendshipId, friendUser);
                            break;
                        case 1:
                            // 帮浇水 - 消耗自己的能量帮好友的番茄+20%进度
                            handleHelpWater(friendUser);
                            break;
                        case 2:
                            // 偷能量 - 从好友那里获取最多50能量
                            handleStealEnergy(friendUser);
                            break;
                        case 3:
                            DialogHelper.showStyledConfirmDialog(requireContext(),
                                    "确认删除",
                                    "确定要删除好友 " + friendUser.getNickname() + " 吗？",
                                    "删除", (dd, dw) -> {
                                        friendRepo.deleteFriend(friendshipId);
                                        refreshContent();
                                    },
                                    "取消", null);
                            break;
                    }
                })
                .show();
    }

    /**
     * 显示好友资料弹窗（MaterialAlertDialogBuilder + 头像 + 短ID + 关系建立时间）
     */
    private void showFriendProfileDialog(int friendshipId, User friendUser) {
        // 后台查询关系建立时间
        new Thread(() -> {
            Friendship fs = friendRepo.findFriendship(currentUser.getUserId(), friendUser.getUserId());
            final String timeText;
            if (fs != null && fs.getUpdatedAt() > 0) {
                java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat(
                        "yyyy-MM-dd HH:mm", java.util.Locale.getDefault());
                timeText = "成为好友: " + sdf.format(new java.util.Date(fs.getUpdatedAt()));
            } else {
                timeText = "成为好友: 未知";
            }

            new Handler(Looper.getMainLooper()).post(() -> {
                if (binding == null) return;
                // 构建自定义视图
        LinearLayout dialogLayout = new LinearLayout(requireContext());
        dialogLayout.setOrientation(LinearLayout.VERTICAL);
        dialogLayout.setPadding(48, 32, 48, 16);
        dialogLayout.setGravity(android.view.Gravity.CENTER_HORIZONTAL);

        // 头像
        ImageView ivAvatar = new ImageView(requireContext());
        ivAvatar.setLayoutParams(new LinearLayout.LayoutParams(120, 120));
        ivAvatar.setScaleType(ImageView.ScaleType.CENTER_CROP);
        String avatarUrl = friendUser.getAvatarUrl();
        if (avatarUrl != null && !avatarUrl.isEmpty()) {
            try {
                Bitmap bmp = BitmapFactory.decodeFile(avatarUrl);
                if (bmp != null) ivAvatar.setImageBitmap(getCircularBitmap(bmp));
            } catch (Exception ignored) {}
        }
        dialogLayout.addView(ivAvatar);

        // 昵称
        TextView tvName = new TextView(requireContext());
        tvName.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        tvName.setText(friendUser.getNickname());
        tvName.setTextSize(20);
        tvName.setTextColor(requireContext().getResources().getColor(
                com.sportytomato.app.R.color.on_surface));
        tvName.setTypeface(null, android.graphics.Typeface.BOLD);
        tvName.setPadding(0, 20, 0, 8);
        tvName.setGravity(android.view.Gravity.CENTER);
        dialogLayout.addView(tvName);

        // 短ID
        TextView tvId = new TextView(requireContext());
        tvId.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        tvId.setText("ID: " + (friendUser.getShortId() != null ? friendUser.getShortId() : "无"));
        tvId.setTextSize(14);
        tvId.setTextColor(requireContext().getResources().getColor(
                com.sportytomato.app.R.color.on_surface_variant));
        tvId.setPadding(0, 0, 0, 8);
        dialogLayout.addView(tvId);

        // 关系建立时间
        TextView tvTime = new TextView(requireContext());
        tvTime.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        tvTime.setText(timeText);
        tvTime.setTextSize(13);
        tvTime.setTextColor(requireContext().getResources().getColor(
                com.sportytomato.app.R.color.on_surface_variant));
        tvTime.setPadding(0, 0, 0, 16);
        dialogLayout.addView(tvTime);

        new com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
                .setView(dialogLayout)
                .setPositiveButton("发消息", (d, w) -> {
                    Intent intent = new Intent(requireContext(), ChatActivity.class);
                    intent.putExtra("friend_user_id", friendUser.getUserId());
                    startActivity(intent);
                })
                .setNeutralButton("关闭", null)
                .show();
            }); // Handler.post
        }).start(); // Thread
    }

    private void handleHelpWater(User friendUser) {
        final RepositoryManager rm = RepositoryManager.getInstance();
        final User myUser = userRepo.getCurrentUser();
        if (myUser == null) {
            Toast.makeText(requireContext(), "请先登录", Toast.LENGTH_SHORT).show();
            return;
        }

        final int myUserId = myUser.getUserId();
        final String myNickname = myUser.getNickname();
        final String myShortId = myUser.getShortId();
        final int friendUserId = friendUser.getUserId();
        final String friendNickname = friendUser.getNickname();
        final String friendShortId = friendUser.getShortId();

        new Thread(() -> {
            int myEnergy = rm.getEnergyRepo().getTodayEnergy(myUserId);
            if (myEnergy < 100) {
                new Handler(Looper.getMainLooper()).post(() ->
                    Toast.makeText(requireContext(), "能量不足（需要100能量，当前：" + myEnergy + "）", Toast.LENGTH_SHORT).show());
                return;
            }

            TomatoRepository tomatoRepo = rm.getTomatoRepo();
            TomatoState friendTomato = tomatoRepo.getTomato(friendUserId);
            if (friendTomato == null) {
                tomatoRepo.createInitialTomato(friendUserId);
                friendTomato = tomatoRepo.getTomato(friendUserId);
            }
            if (friendTomato == null) {
                new Handler(Looper.getMainLooper()).post(() ->
                    Toast.makeText(requireContext(), "创建番茄失败", Toast.LENGTH_SHORT).show());
                return;
            }
            if (friendTomato.getProgress() >= 100) {
                new Handler(Looper.getMainLooper()).post(() ->
                    Toast.makeText(requireContext(), "好友的番茄已经成熟了，等他收获后才可以浇水", Toast.LENGTH_SHORT).show());
                return;
            }

            rm.getEnergyRepo().consumeEnergyWithType(myUserId, 100, 3);
            if (friendTomato.getProgress() >= 100) {
                new Handler(Looper.getMainLooper()).post(() ->
                    Toast.makeText(requireContext(), "好友的番茄已经成熟了，等他收获后才可以浇水", Toast.LENGTH_SHORT).show());
                return;
            }
            int newProgress = Math.min(friendTomato.getProgress() + 20, 100);
            int newStage = newProgress < 20 ? 0 : newProgress < 60 ? 1 : newProgress < 100 ? 2 : 3;
            tomatoRepo.updateProgressDirect(friendTomato.getTomatoId(), newProgress, newStage);

            // 发送系统消息
            sendSystemMessage(friendUserId,
                    myNickname + " 帮 " + friendNickname + " 的番茄浇了水",
                    null,
                    1);

            // 后台推送到EMAS云端
            pushWateringToCloud(myShortId, friendShortId);

            new Handler(Looper.getMainLooper()).post(() -> {
                Toast.makeText(requireContext(), "已帮" + friendNickname + "浇水！", Toast.LENGTH_SHORT).show();
                refreshContent();
            });
        }).start();
    }

    private void pushWateringToCloud(String fromShortId, String toShortId) {
        if (!EMASClient.getInstance().isInitialized()) return;
        new Thread(() -> {
            try {
                JSONObject waterLog = new JSONObject();
                waterLog.put("fromShortId", fromShortId);
                waterLog.put("toShortId", toShortId);
                waterLog.put("waterAmount", 20);
                waterLog.put("wateredAt", System.currentTimeMillis());

                String params = EMASClient.buildDbParams("insertOne", "watering_logs",
                        waterLog.toString(), null);
                EMASClient.getInstance().callFunction("sporty-api", params);
                android.util.Log.i("FriendsFrag", "浇水记录已推送到云端");
            } catch (Exception e) {
                android.util.Log.e("FriendsFrag", "浇水记录推送异常: " + e.getMessage());
            }
        }).start();
    }

    private void handleStealEnergy(User friendUser) {
        final RepositoryManager rm = RepositoryManager.getInstance();
        final User myUser = userRepo.getCurrentUser();
        if (myUser == null) {
            Toast.makeText(requireContext(), "请先登录", Toast.LENGTH_SHORT).show();
            return;
        }

        final int myUserId = myUser.getUserId();
        final String myNickname = myUser.getNickname();
        final String myShortId = myUser.getShortId();
        final int friendUserId = friendUser.getUserId();
        final String friendNickname = friendUser.getNickname();
        final String friendShortId = friendUser.getShortId();

        new Thread(() -> {
            int friendEnergy = rm.getEnergyRepo().getTodayEnergy(friendUserId);
            if (friendEnergy <= 0) {
                new Handler(Looper.getMainLooper()).post(() ->
                    Toast.makeText(requireContext(), "好友今日没有多余能量", Toast.LENGTH_SHORT).show());
                return;
            }

            int stealAmount = Math.min(50, friendEnergy);
            rm.getEnergyRepo().consumeEnergy(friendUserId, stealAmount);
            rm.getEnergyRepo().addSteps(myUserId, stealAmount * 10);

            // 发送系统消息
            sendSystemMessage(friendUserId,
                    myNickname + " 从 " + friendNickname + " 偷取了 " + stealAmount + " 能量",
                    null,
                    2);

            // 后台推送到EMAS云端
            pushStealEnergyToCloud(myShortId, friendShortId, stealAmount);

            new Handler(Looper.getMainLooper()).post(() ->
                Toast.makeText(requireContext(), "从" + friendNickname + "偷取了 " + stealAmount + " 能量！", Toast.LENGTH_SHORT).show());
        }).start();
    }

    private void pushStealEnergyToCloud(String fromShortId, String toShortId, int amount) {
        if (!EMASClient.getInstance().isInitialized()) return;
        new Thread(() -> {
            try {
                JSONObject energyRecord = new JSONObject();
                energyRecord.put("fromShortId", fromShortId);
                energyRecord.put("toShortId", toShortId);
                energyRecord.put("amount", amount);
                energyRecord.put("type", "steal");
                energyRecord.put("createdAt", System.currentTimeMillis());

                String params = EMASClient.buildDbParams("insertOne", "energy_records",
                        energyRecord.toString(), null);
                EMASClient.getInstance().callFunction("sporty-api", params);
                android.util.Log.i("FriendsFrag", "偷能量记录已推送到云端");
            } catch (Exception e) {
                android.util.Log.e("FriendsFrag", "偷能量记录推送异常: " + e.getMessage());
            }
        }).start();
    }

    /**
     * 发送系统消息：仅写入"我→好友"单向消息
     * （聊天界面展示双向消息，好友也能看到这条"我帮XXX"的消息，理解语义即可）
     */
    private void sendSystemMessage(int friendUserId, String fromMeText, String fromFriendText, int type) {
        if (currentUser == null || messageRepo == null) return;
        messageRepo.sendMessage(Message.createSystem(currentUser.getUserId(), friendUserId, fromMeText, type));
    }

    private int getOtherUserId(Friendship f) {
        return currentUser != null && f.getUserId() == currentUser.getUserId()
                ? f.getFriendUserId() : f.getUserId();
    }

    private void addEmptyView(LinearLayout content, String text) {
        TextView tv = new TextView(requireContext());
        tv.setText(text);
        tv.setTextSize(14);
        tv.setTextColor(requireContext().getResources().getColor(
                com.sportytomato.app.R.color.on_surface_variant));
        tv.setGravity(android.view.Gravity.CENTER);
        tv.setPadding(0, 64, 0, 0);
        content.addView(tv);
    }

    /**
     * 更新底部导航好友Tab的未读消息小红点
     */
    private void updateUnreadBadge() {
        if (getActivity() == null || currentUser == null || messageRepo == null) return;
        new Thread(() -> {
            try {
                int unreadCount = messageRepo.getUnreadMessages(currentUser.getUserId()).size();
                new Handler(Looper.getMainLooper()).post(() -> {
                    if (getActivity() instanceof MainActivity) {
                        ((MainActivity) getActivity()).updateFriendsBadge(unreadCount);
                    }
                });
            } catch (Exception ignored) {}
        }).start();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (userRepo == null || binding == null) return;
        new Thread(() -> {
            currentUser = userRepo.getCurrentUser();
            if (currentUser != null) {
                new Handler(Looper.getMainLooper()).post(() -> {
                    if (binding == null) return;
                    refreshContent();
                    updateUnreadBadge();
                    syncIfNeeded();
                });
            }
        }).start();
    }

    private void syncIfNeeded() {
        if (EMASClient.getInstance().isInitialized()
                && (System.currentTimeMillis() - lastCloudSyncTime) > 30_000) {
            syncFriendsFromCloud(true);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    /**
     * 好友成就检测：接受好友后检查是否解锁"好友达人"勋章 (triggerType=6)
     */
    private void checkFriendAchievement() {
        try {
            if (currentUser == null) return;
            final int userId = currentUser.getUserId();
            new Thread(() -> {
                java.util.List<com.sportytomato.app.model.entity.Friendship> accepted =
                        friendRepo.getAcceptedFriendships(userId);
                int friendCount = accepted != null ? accepted.size() : 0;
                SportyTomatoApplication app = (SportyTomatoApplication) requireActivity().getApplication();
                app.getAchievementService().checkAndUnlock(userId, 6, friendCount);
            }).start();
        } catch (Exception ignored) {}
    }

    private Bitmap getCircularBitmap(Bitmap source) {
        if (source == null) return null;
        int size = Math.min(source.getWidth(), source.getHeight());
        Bitmap output = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(output);
        Paint paint = new Paint();
        paint.setAntiAlias(true);
        BitmapShader shader = new BitmapShader(source, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP);
        paint.setShader(shader);
        float radius = size / 2f;
        canvas.drawCircle(radius, radius, radius, paint);
        return output;
    }
}
