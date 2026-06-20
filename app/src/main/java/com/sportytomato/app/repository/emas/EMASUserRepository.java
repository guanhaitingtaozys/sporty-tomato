package com.sportytomato.app.repository.emas;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.sportytomato.app.model.entity.User;
import com.sportytomato.app.repository.UserRepository;
import com.sportytomato.app.service.EMASClient;

import org.json.JSONArray;
import org.json.JSONObject;

import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * EMAS Serverless 实现的 UserRepository
 */
public class EMASUserRepository implements UserRepository {

    private static final String COLLECTION = "users";
    private static final String FUNCTION_NAME = "sporty-api";

    private final EMASClient client;
    private final ExecutorService executor;
    private final MutableLiveData<User> currentUserLive = new MutableLiveData<>();
    private User cachedCurrentUser;

    private static volatile EMASUserRepository instance;

    public static EMASUserRepository getInstance() {
        if (instance == null) {
            synchronized (EMASUserRepository.class) {
                if (instance == null) {
                    instance = new EMASUserRepository();
                }
            }
        }
        return instance;
    }

    public EMASUserRepository() {
        this.client = EMASClient.getInstance();
        this.executor = Executors.newSingleThreadExecutor();
    }

    @Override
    public User getCurrentUser() {
        return cachedCurrentUser;
    }

    @Override
    public LiveData<User> getCurrentUserLive() {
        return currentUserLive;
    }

    @Override
    public void createGuestUser(String localId, String nickname) {
        executor.execute(() -> {
            try {
                String shortId = generateNextShortId();
                long now = System.currentTimeMillis();

                JSONObject data = new JSONObject();
                data.put("localId", localId);
                data.put("nickname", nickname);
                data.put("avatarUrl", "");
                data.put("shortId", shortId);
                data.put("userType", 0);
                data.put("createdAt", now);
                data.put("updatedAt", now);
                data.put("deleted", 0);

                String params = EMASClient.buildDbParams("insertOne", COLLECTION,
                        data.toString(), null);
                String resultStr = client.callFunction(FUNCTION_NAME, params);

                if (resultStr != null) {
                    User user = new User();
                    user.setUserId(parseUserIdFromInsertResult(resultStr));
                    user.setLocalId(localId);
                    user.setNickname(nickname);
                    user.setAvatarUrl("");
                    user.setShortId(shortId);
                    user.setUserType(0);
                    user.setCreatedAt(now);
                    user.setUpdatedAt(now);
                    user.setDeleted(0);
                    cachedCurrentUser = user;
                    currentUserLive.postValue(user);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    @Override
    public void updateNickname(int userId, String nickname) {
        executor.execute(() -> {
            try {
                JSONObject query = new JSONObject();
                query.put("userId", userId);

                JSONObject data = new JSONObject();
                JSONObject set = new JSONObject();
                set.put("nickname", nickname);
                data.put("$set", set);

                String params = EMASClient.buildDbParams("updateOne", COLLECTION,
                        data.toString(), query.toString());
                client.callFunction(FUNCTION_NAME, params);

                if (cachedCurrentUser != null && cachedCurrentUser.getUserId() == userId) {
                    cachedCurrentUser.setNickname(nickname);
                    currentUserLive.postValue(cachedCurrentUser);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    @Override
    public User registerByPhone(String phone, String password, String nickname) {
        try {
            // 检查是否已注册
            JSONObject checkQuery = new JSONObject();
            checkQuery.put("phone", phone);
            JSONObject checkOptions = new JSONObject();
            checkOptions.put("limit", 1);
            String checkParams = EMASClient.buildDbParamsWithOptions("find", COLLECTION,
                    null, checkQuery.toString(), checkOptions.toString());
            String checkResult = client.callFunction(FUNCTION_NAME, checkParams);
            if (checkResult != null) {
                JSONArray existing = parseResultArray(checkResult);
                if (existing != null && existing.length() > 0) {
                    return null; // 已注册
                }
            }

            // 创建新用户
            String shortId = generateNextShortId();
            long now = System.currentTimeMillis();
            String passwordHash = hashPassword(password);

            JSONObject data = new JSONObject();
            data.put("phone", phone);
            data.put("passwordHash", passwordHash);
            data.put("nickname", nickname);
            data.put("avatarUrl", "");
            data.put("shortId", shortId);
            data.put("userType", 2);
            data.put("createdAt", now);
            data.put("updatedAt", now);
            data.put("deleted", 0);

            String insertParams = EMASClient.buildDbParams("insertOne", COLLECTION,
                    data.toString(), null);
            String insertResult = client.callFunction(FUNCTION_NAME, insertParams);

            if (insertResult != null) {
                User user = new User();
                user.setUserId(parseUserIdFromInsertResult(insertResult));
                user.setPhone(phone);
                user.setPasswordHash(passwordHash);
                user.setNickname(nickname);
                user.setAvatarUrl("");
                user.setShortId(shortId);
                user.setUserType(2);
                user.setCreatedAt(now);
                user.setUpdatedAt(now);
                user.setDeleted(0);
                cachedCurrentUser = user;
                currentUserLive.postValue(user);
                return user;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public User loginByPhone(String phone, String password) {
        try {
            JSONObject query = new JSONObject();
            query.put("phone", phone);
            JSONObject options = new JSONObject();
            options.put("limit", 1);

            String params = EMASClient.buildDbParamsWithOptions("find", COLLECTION,
                    null, query.toString(), options.toString());
            String result = client.callFunction(FUNCTION_NAME, params);

            if (result != null) {
                JSONArray users = parseResultArray(result);
                if (users != null && users.length() > 0) {
                    User user = parseUserJson(users.getJSONObject(0));
                    if (user != null) {
                        String inputHash = hashPassword(password);
                        if (inputHash.equals(user.getPasswordHash())) {
                            cachedCurrentUser = user;
                            currentUserLive.postValue(user);
                            return user;
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public User findByPhone(String phone) {
        try {
            JSONObject query = new JSONObject();
            query.put("phone", phone);
            JSONObject options = new JSONObject();
            options.put("limit", 1);

            String params = EMASClient.buildDbParamsWithOptions("find", COLLECTION,
                    null, query.toString(), options.toString());
            String result = client.callFunction(FUNCTION_NAME, params);

            if (result != null) {
                JSONArray users = parseResultArray(result);
                if (users != null && users.length() > 0) {
                    return parseUserJson(users.getJSONObject(0));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public void logout() {
        cachedCurrentUser = null;
        currentUserLive.postValue(null);
    }

    @Override
    public User findByShortId(String shortId) {
        try {
            JSONObject query = new JSONObject();
            query.put("shortId", shortId);

            JSONObject options = new JSONObject();
            options.put("limit", 1);

            String params = EMASClient.buildDbParamsWithOptions("find", COLLECTION,
                    null, query.toString(), options.toString());
            String result = client.callFunction(FUNCTION_NAME, params);

            if (result != null) {
                JSONArray users = parseResultArray(result);
                if (users != null && users.length() > 0) {
                    return parseUserJson(users.getJSONObject(0));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public User getUserById(int userId) {
        try {
            JSONObject query = new JSONObject();
            query.put("userId", userId);

            JSONObject options = new JSONObject();
            options.put("limit", 1);

            String params = EMASClient.buildDbParamsWithOptions("find", COLLECTION,
                    null, query.toString(), options.toString());
            String result = client.callFunction(FUNCTION_NAME, params);

            if (result != null) {
                JSONArray users = parseResultArray(result);
                if (users != null && users.length() > 0) {
                    return parseUserJson(users.getJSONObject(0));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public void updatePhone(int userId, String phone) {
        executor.execute(() -> {
            try {
                JSONObject query = new JSONObject();
                query.put("userId", userId);

                JSONObject data = new JSONObject();
                JSONObject set = new JSONObject();
                set.put("phone", phone);
                set.put("updatedAt", System.currentTimeMillis());
                data.put("$set", set);

                String params = EMASClient.buildDbParams("updateOne", COLLECTION,
                        data.toString(), query.toString());
                client.callFunction(FUNCTION_NAME, params);

                if (cachedCurrentUser != null && cachedCurrentUser.getUserId() == userId) {
                    cachedCurrentUser.setPhone(phone);
                    currentUserLive.postValue(cachedCurrentUser);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    @Override
    public void updateAvatarUrl(int userId, String avatarUrl) {
        executor.execute(() -> {
            try {
                JSONObject query = new JSONObject();
                query.put("userId", userId);

                JSONObject data = new JSONObject();
                JSONObject set = new JSONObject();
                set.put("avatarUrl", avatarUrl);
                set.put("updatedAt", System.currentTimeMillis());
                data.put("$set", set);

                String params = EMASClient.buildDbParams("updateOne", COLLECTION,
                        data.toString(), query.toString());
                client.callFunction(FUNCTION_NAME, params);

                if (cachedCurrentUser != null && cachedCurrentUser.getUserId() == userId) {
                    cachedCurrentUser.setAvatarUrl(avatarUrl);
                    currentUserLive.postValue(cachedCurrentUser);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    @Override
    public void deleteAccount(int userId) {
        // EMAS 客户端仅操作云端，本地删除由 UserRepositoryImpl 处理
    }

    @Override
    public User ensureLocalUser(User remoteUser) {
        // EMAS 仅返回远程数据，不需要本地同步
        return remoteUser;
    }

    @Override
    public String getLastError() {
        return null;
    }

    @Override
    public String getTodayDate() {
        return new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
    }

    // ========== 工具方法 ==========

    /** 生成递增的 6 位 shortId（通过查询现有最大 ID + 1） */
    private String generateNextShortId() {
        try {
            // 查询最大的 shortId
            JSONObject options = new JSONObject();
            options.put("sort", new JSONObject().put("shortId", -1));
            options.put("limit", 1);
            options.put("projection", new JSONObject().put("shortId", 1));

            String params = EMASClient.buildDbParamsWithOptions("find", COLLECTION,
                    null, "{}", options.toString());
            String result = client.callFunction(FUNCTION_NAME, params);

            if (result != null) {
                JSONArray users = parseResultArray(result);
                if (users != null && users.length() > 0) {
                    String maxId = users.getJSONObject(0).optString("shortId", "0");
                    try {
                        int next = Integer.parseInt(maxId) + 1;
                        if (next > 999999) next = 0;
                        return String.format("%06d", next);
                    } catch (NumberFormatException ignored) {}
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "000000";
    }

    private String hashPassword(String password) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest((password + "_sporty_tomato_salt_").getBytes("UTF-8"));
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) hex.append(String.format("%02x", b));
            return hex.toString();
        } catch (Exception e) {
            return password;
        }
    }

    /**
     * 从 insertOne 返回值解析插入的文档 ID（用于构造本地 User 对象）
     */
    private int parseUserIdFromInsertResult(String result) {
        try {
            JSONObject root = new JSONObject(result);
            if (root.has("result")) {
                Object res = root.get("result");
                if (res instanceof JSONObject) {
                    String insertedId = ((JSONObject) res).optString("insertedId", null);
                    if (insertedId != null && !insertedId.isEmpty()) {
                        return Math.abs(insertedId.hashCode());
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return (int) (System.currentTimeMillis() % Integer.MAX_VALUE);
    }

    /**
     * 从 insertOne 返回值解析 User（insert 只返回 insertedId，不返回完整文档）
     */
    private String parseInsertedId(String result) {
        try {
            JSONObject root = new JSONObject(result);
            if (root.has("result")) {
                Object res = root.get("result");
                if (res instanceof JSONObject) {
                    return ((JSONObject) res).optString("insertedId", null);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private JSONArray parseResultArray(String result) {
        try {
            JSONObject root = new JSONObject(result);
            if (root.has("result")) {
                Object res = root.get("result");
                if (res instanceof JSONArray) {
                    return (JSONArray) res;
                } else if (res instanceof JSONObject) {
                    JSONObject resObj = (JSONObject) res;
                    if (resObj.has("data")) {
                        return resObj.getJSONArray("data");
                    }
                }
            }
            if (root.has("data")) {
                Object data = root.get("data");
                if (data instanceof JSONArray) return (JSONArray) data;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public User parseUserJson(JSONObject json) {
        try {
            User user = new User();
            user.setUserId(json.optInt("userId", 0));
            if (json.has("openId")) {
                user.setOpenId(json.optString("openId", null));
            }
            if (json.has("localId")) {
                user.setLocalId(json.optString("localId", null));
            }
            user.setNickname(json.optString("nickname", ""));
            user.setAvatarUrl(json.optString("avatarUrl", ""));
            user.setShortId(json.optString("shortId", ""));
            user.setUserType(json.optInt("userType", 0));
            if (json.has("unionId")) {
                user.setUnionId(json.optString("unionId", null));
            }
            if (json.has("wxNickname")) {
                user.setWxNickname(json.optString("wxNickname", null));
            }
            if (json.has("wxAvatar")) {
                user.setWxAvatar(json.optString("wxAvatar", null));
            }
            if (json.has("phone")) {
                user.setPhone(json.optString("phone", null));
            }
            if (json.has("passwordHash")) {
                user.setPasswordHash(json.optString("passwordHash", null));
            }
            user.setCreatedAt(json.optLong("createdAt", System.currentTimeMillis()));
            user.setUpdatedAt(json.optLong("updatedAt", System.currentTimeMillis()));
            user.setDeleted(json.optInt("deleted", 0));
            return user;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
