package com.sportytomato.app.repository.mock;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.sportytomato.app.model.entity.User;
import com.sportytomato.app.repository.UserRepository;

import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class MockUserRepository implements UserRepository {

    private final ConcurrentHashMap<Integer, User> userStore = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, User> byShortId = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, User> byPhone = new ConcurrentHashMap<>();
    private final AtomicInteger nextId = new AtomicInteger(1);
    private final AtomicInteger nextShortId = new AtomicInteger(0);
    private int currentUserId = -1;

    @Override
    public User getCurrentUser() {
        if (currentUserId == -1) return null;
        return userStore.get(currentUserId);
    }

    @Override
    public LiveData<User> getCurrentUserLive() {
        MutableLiveData<User> liveData = new MutableLiveData<>();
        liveData.setValue(getCurrentUser());
        return liveData;
    }

    @Override
    public void createGuestUser(String localId, String nickname) {
        long now = System.currentTimeMillis();
        int userId = nextId.getAndIncrement();
        String effectiveLocalId = (localId != null && !localId.isEmpty()) ? localId : UUID.randomUUID().toString();
        String shortId = allocateShortId();

        User user = new User();
        user.setUserId(userId);
        user.setLocalId(effectiveLocalId);
        user.setNickname(nickname);
        user.setShortId(shortId);
        user.setUserType(0);
        user.setDeleted(0);
        user.setCreatedAt(now);
        user.setUpdatedAt(now);

        userStore.put(userId, user);
        byShortId.put(shortId, user);
        currentUserId = userId;
    }

    @Override
    public void updateNickname(int userId, String nickname) {
        User user = userStore.get(userId);
        if (user != null) {
            user.setNickname(nickname);
            user.setUpdatedAt(System.currentTimeMillis());
        }
    }

    @Override
    public User registerByPhone(String phone, String password, String nickname) {
        if (byPhone.containsKey(phone)) {
            return null; // 已注册
        }
        long now = System.currentTimeMillis();
        int userId = nextId.getAndIncrement();
        String shortId = allocateShortId();

        User user = new User();
        user.setUserId(userId);
        user.setPhone(phone);
        user.setPasswordHash(hashPassword(password));
        user.setNickname(nickname);
        user.setShortId(shortId);
        user.setUserType(2);
        user.setDeleted(0);
        user.setCreatedAt(now);
        user.setUpdatedAt(now);

        userStore.put(userId, user);
        byShortId.put(shortId, user);
        byPhone.put(phone, user);
        currentUserId = userId;
        return user;
    }

    @Override
    public User loginByPhone(String phone, String password) {
        User user = byPhone.get(phone);
        if (user == null) return null;
        if (!hashPassword(password).equals(user.getPasswordHash())) return null;
        currentUserId = user.getUserId();
        return user;
    }

    @Override
    public User findByPhone(String phone) {
        return byPhone.get(phone);
    }

    @Override
    public void logout() {
        currentUserId = -1;
    }

    @Override
    public User findByShortId(String shortId) {
        return byShortId.get(shortId);
    }

    @Override
    public User getUserById(int userId) {
        return userStore.get(userId);
    }

    @Override
    public void updatePhone(int userId, String phone) {
        User user = userStore.get(userId);
        if (user != null) {
            String oldPhone = user.getPhone();
            if (oldPhone != null) byPhone.remove(oldPhone);
            user.setPhone(phone);
            user.setUpdatedAt(System.currentTimeMillis());
            byPhone.put(phone, user);
        }
    }

    @Override
    public void updateAvatarUrl(int userId, String avatarUrl) {
        User user = userStore.get(userId);
        if (user != null) {
            user.setAvatarUrl(avatarUrl);
            user.setUpdatedAt(System.currentTimeMillis());
        }
    }

    @Override
    public void deleteAccount(int userId) {
        userStore.remove(userId);
        currentUserId = -1;
    }

    @Override
    public User ensureLocalUser(User remoteUser) {
        // Mock 不需要本地同步
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

    private String allocateShortId() {
        int id = nextShortId.getAndIncrement();
        return String.format("%06d", id);
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
}
