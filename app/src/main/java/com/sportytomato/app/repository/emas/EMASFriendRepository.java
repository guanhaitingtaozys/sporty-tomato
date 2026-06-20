package com.sportytomato.app.repository.emas;

import com.sportytomato.app.model.entity.Friendship;
import com.sportytomato.app.model.entity.User;
import com.sportytomato.app.repository.FriendRepository;
import com.sportytomato.app.service.EMASClient;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * EMAS Serverless 实现的 FriendRepository
 */
public class EMASFriendRepository implements FriendRepository {

    private static final String COLLECTION = "friendships";
    private static final String FUNCTION_NAME = "sporty-api";

    private final EMASClient client;
    private final ExecutorService executor;

    public EMASFriendRepository() {
        this.client = EMASClient.getInstance();
        this.executor = Executors.newSingleThreadExecutor();
    }

    @Override
    public Friendship sendRequest(int fromUserId, int toUserId, String message) {
        try {
            Friendship friendship = Friendship.createRequest(fromUserId, toUserId, message);

            JSONObject data = new JSONObject();
            data.put("userId", friendship.getUserId());
            data.put("friendUserId", friendship.getFriendUserId());
            data.put("status", friendship.getStatus());
            data.put("verifyMessage", friendship.getVerifyMessage() != null ? friendship.getVerifyMessage() : "");
            data.put("createdAt", friendship.getCreatedAt());
            data.put("updatedAt", friendship.getUpdatedAt());

            String params = EMASClient.buildDbParams("insertOne", COLLECTION,
                    data.toString(), null);
            String result = client.callFunction(FUNCTION_NAME, params);

            if (result != null) {
                return parseFriendshipFromResult(result);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public void acceptRequest(int friendshipId) {
        try {
            JSONObject query = new JSONObject();
            query.put("id", friendshipId);

            JSONObject data = new JSONObject();
            JSONObject set = new JSONObject();
            set.put("status", 1);
            set.put("updatedAt", System.currentTimeMillis());
            data.put("$set", set);

            String params = EMASClient.buildDbParams("updateOne", COLLECTION,
                    data.toString(), query.toString());
            client.callFunction(FUNCTION_NAME, params);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void acceptRequestWithTime(int friendshipId, long updatedAt) {
        try {
            JSONObject query = new JSONObject();
            query.put("id", friendshipId);

            JSONObject data = new JSONObject();
            JSONObject set = new JSONObject();
            set.put("status", 1);
            set.put("updatedAt", updatedAt);
            data.put("$set", set);

            String params = EMASClient.buildDbParams("updateOne", COLLECTION,
                    data.toString(), query.toString());
            client.callFunction(FUNCTION_NAME, params);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void rejectRequest(int friendshipId) {
        try {
            JSONObject query = new JSONObject();
            query.put("id", friendshipId);

            JSONObject data = new JSONObject();
            JSONObject set = new JSONObject();
            set.put("status", 2);
            set.put("updatedAt", System.currentTimeMillis());
            data.put("$set", set);

            String params = EMASClient.buildDbParams("updateOne", COLLECTION,
                    data.toString(), query.toString());
            client.callFunction(FUNCTION_NAME, params);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void deleteFriend(int friendshipId) {
        try {
            JSONObject query = new JSONObject();
            query.put("id", friendshipId);

            String params = EMASClient.buildDbParams("deleteOne", COLLECTION,
                    null, query.toString());
            client.callFunction(FUNCTION_NAME, params);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public List<Friendship> getAcceptedFriendships(int userId) {
        List<Friendship> result = new ArrayList<>();
        try {
            JSONObject query = new JSONObject();
            query.put("status", 1);

            JSONObject mainQuery = new JSONObject();
            JSONArray orArray = new JSONArray();

            JSONObject cond1 = new JSONObject();
            cond1.put("userId", userId);
            orArray.put(cond1);

            JSONObject cond2 = new JSONObject();
            cond2.put("friendUserId", userId);
            orArray.put(cond2);

            mainQuery.put("$or", orArray);
            mainQuery.put("status", 1);

            String params = EMASClient.buildDbParamsWithOptions("find", COLLECTION,
                    null, mainQuery.toString(), null);
            String response = client.callFunction(FUNCTION_NAME, params);

            if (response != null) {
                JSONArray arr = parseResultArray(response);
                if (arr != null) {
                    for (int i = 0; i < arr.length(); i++) {
                        Friendship f = parseFriendshipJson(arr.getJSONObject(i));
                        if (f != null) {
                            result.add(f);
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    @Override
    public List<Friendship> getPendingRequests(int userId) {
        List<Friendship> result = new ArrayList<>();
        try {
            JSONObject query = new JSONObject();
            query.put("friendUserId", userId);
            query.put("status", 0);

            String params = EMASClient.buildDbParamsWithOptions("find", COLLECTION,
                    null, query.toString(), null);
            String response = client.callFunction(FUNCTION_NAME, params);

            if (response != null) {
                JSONArray arr = parseResultArray(response);
                if (arr != null) {
                    for (int i = 0; i < arr.length(); i++) {
                        Friendship f = parseFriendshipJson(arr.getJSONObject(i));
                        if (f != null) {
                            result.add(f);
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    @Override
    public int getPendingCount(int userId) {
        try {
            JSONObject query = new JSONObject();
            query.put("friendUserId", userId);
            query.put("status", 0);

            String params = EMASClient.buildDbParams("count", COLLECTION,
                    null, query.toString());
            String response = client.callFunction(FUNCTION_NAME, params);

            if (response != null) {
                JSONObject root = new JSONObject(response);
                JSONObject resObj = root.optJSONObject("result");
                if (resObj != null) {
                    return resObj.optInt("total", 0);
                }
                return root.optInt("total", 0);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }

    @Override
    public boolean areFriends(int a, int b) {
        return findFriendship(a, b) != null;
    }

    @Override
    public List<Integer> getFriendIds(int userId) {
        List<Integer> result = new ArrayList<>();
        List<Friendship> friends = getAcceptedFriendships(userId);
        for (Friendship f : friends) {
            if (f.getUserId() == userId) {
                result.add(f.getFriendUserId());
            } else {
                result.add(f.getUserId());
            }
        }
        return result;
    }

    @Override
    public List<User> getFriendUsers(int userId) {
        List<User> result = new ArrayList<>();
        try {
            List<Integer> friendIds = getFriendIds(userId);
            for (int fid : friendIds) {
                JSONObject query = new JSONObject();
                query.put("userId", fid);

                JSONObject options = new JSONObject();
                options.put("limit", 1);

                String params = EMASClient.buildDbParamsWithOptions("find", "users",
                        null, query.toString(), options.toString());
                String response = client.callFunction(FUNCTION_NAME, params);

                if (response != null) {
                    JSONArray arr = parseResultArray(response);
                    if (arr != null && arr.length() > 0) {
                        User user = parseUserJson(arr.getJSONObject(0));
                        if (user != null) {
                            result.add(user);
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    @Override
    public Friendship findFriendship(int userIdA, int userIdB) {
        try {
            JSONObject query = new JSONObject();

            JSONArray orArray = new JSONArray();

            JSONObject cond1 = new JSONObject();
            cond1.put("userId", userIdA);
            cond1.put("friendUserId", userIdB);
            orArray.put(cond1);

            JSONObject cond2 = new JSONObject();
            cond2.put("userId", userIdB);
            cond2.put("friendUserId", userIdA);
            orArray.put(cond2);

            query.put("$or", orArray);
            query.put("status", 1);

            JSONObject options = new JSONObject();
            options.put("limit", 1);

            String params = EMASClient.buildDbParamsWithOptions("find", COLLECTION,
                    null, query.toString(), options.toString());
            String response = client.callFunction(FUNCTION_NAME, params);

            if (response != null) {
                JSONArray arr = parseResultArray(response);
                if (arr != null && arr.length() > 0) {
                    return parseFriendshipJson(arr.getJSONObject(0));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    // ========== 工具方法 ==========

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

    private Friendship parseFriendshipFromResult(String result) {
        try {
            JSONObject root = new JSONObject(result);
            if (root.has("_id")) {
                return parseFriendshipJson(root);
            }
            if (root.has("result")) {
                JSONObject resObj = root.getJSONObject("result");
                if (resObj.has("_id")) {
                    return parseFriendshipJson(resObj);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private Friendship parseFriendshipJson(JSONObject json) {
        try {
            Friendship f = new Friendship();
            f.setId(json.optInt("id", json.optInt("_id", 0)));
            f.setUserId(json.optInt("userId", 0));
            f.setFriendUserId(json.optInt("friendUserId", 0));
            f.setStatus(json.optInt("status", 0));
            f.setVerifyMessage(json.optString("verifyMessage", ""));
            f.setCreatedAt(json.optLong("createdAt", System.currentTimeMillis()));
            f.setUpdatedAt(json.optLong("updatedAt", System.currentTimeMillis()));
            return f;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private User parseUserJson(JSONObject json) {
        try {
            User user = new User();
            user.setUserId(json.optInt("userId", 0));
            user.setNickname(json.optString("nickname", ""));
            user.setAvatarUrl(json.optString("avatarUrl", ""));
            user.setShortId(json.optString("shortId", ""));
            user.setUserType(json.optInt("userType", 0));
            return user;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
