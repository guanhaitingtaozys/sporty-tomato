package com.sportytomato.app.repository;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Base64;
import androidx.lifecycle.LiveData;
import com.sportytomato.app.model.database.AppDatabase;
import com.sportytomato.app.model.entity.User;
import com.sportytomato.app.repository.emas.EMASUserRepository;
import com.sportytomato.app.service.EMASClient;
import com.sportytomato.app.service.IdAllocator;
import java.io.File;
import java.io.FileOutputStream;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class UserRepositoryImpl implements UserRepository {
    private static final String PREFS_NAME = "user_prefs";
    private static final String KEY_CURRENT_USER_ID = "current_user_id";
    private final AppDatabase database;
    private final SharedPreferences prefs;
    private final Context appContext;
    private final ExecutorService executor;
    private String lastError = null;

    public UserRepositoryImpl(AppDatabase database, Context context) {
        this.database = database;
        this.appContext = context.getApplicationContext();
        this.prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        this.executor = Executors.newSingleThreadExecutor();
    }

    @Override public User getCurrentUser() { int userId = prefs.getInt(KEY_CURRENT_USER_ID, -1); return userId == -1 ? null : database.userDao().getUserById(userId); }
    @Override public LiveData<User> getCurrentUserLive() { int userId = prefs.getInt(KEY_CURRENT_USER_ID, -1); return userId == -1 ? null : database.userDao().getUserByIdLive(userId); }
    @Override public void createGuestUser(String localId, String nickname) {
        long now = System.currentTimeMillis(); String shortId = IdAllocator.allocate(appContext);
        User user = new User(); user.setLocalId(localId); user.setNickname(nickname); user.setShortId(shortId); user.setCreatedAt(now); user.setUpdatedAt(now); user.setUserType(0); user.setDeleted(0);
        database.userDao().insertUser(user);
        User inserted = database.userDao().getUserByLocalId(localId);
        if (inserted != null) prefs.edit().putInt(KEY_CURRENT_USER_ID, inserted.getUserId()).apply();
    }
    @Override public void updateNickname(int userId, String nickname) { executor.execute(() -> database.userDao().updateNickname(userId, nickname, System.currentTimeMillis())); }
    @Override public String getLastError() { return lastError; }
    @Override public User registerByPhone(String phone, String password, String nickname) {
        lastError = null;
        User existing = database.userDao().getUserByPhone(phone);
        if (existing != null) { lastError = "本地已存在该手机号"; return null; }
        if (EMASClient.getInstance().isInitialized()) {
            try { EMASUserRepository emasRepo = EMASUserRepository.getInstance(); User cloudUser = emasRepo.findByPhone(phone);
                if (cloudUser != null) {
                    if (cloudUser.getPasswordHash() != null && !cloudUser.getPasswordHash().isEmpty()) {
                        if (verifyPassword(password, cloudUser.getPasswordHash())) {
                            cloudUser = ensureLocalUser(cloudUser); if (cloudUser != null) prefs.edit().putInt(KEY_CURRENT_USER_ID, cloudUser.getUserId()).apply(); return cloudUser;
                        } else { lastError = "云端已有该手机号（密码不匹配），请直接登录"; return null; }
                    } else { lastError = "云端已有该手机号，请使用手机号登录"; return null; }
                }
            } catch (Exception e) { lastError = "云端验证失败，请检查网络后重试"; return null; }
        }
        long now = System.currentTimeMillis(); String shortId = IdAllocator.allocate(appContext);
        User user = new User(); user.setPhone(phone); user.setPasswordHash(generateSaltedHash(password)); user.setNickname(nickname); user.setShortId(shortId); user.setUserType(2); user.setDeleted(0); user.setCreatedAt(now); user.setUpdatedAt(now);
        database.userDao().insertUser(user);
        User inserted = database.userDao().getUserByPhone(phone);
        if (inserted != null) prefs.edit().putInt(KEY_CURRENT_USER_ID, inserted.getUserId()).apply();
        return inserted;
    }
    @Override public User loginByPhone(String phone, String password) {
        lastError = null;
        User user = database.userDao().getUserByPhone(phone);
        if (user != null) { if (!verifyPassword(password, user.getPasswordHash())) { lastError = "密码错误"; return null; } prefs.edit().putInt(KEY_CURRENT_USER_ID, user.getUserId()).apply(); return user; }
        if (EMASClient.getInstance().isInitialized()) {
            try { EMASUserRepository emasRepo = EMASUserRepository.getInstance(); User cloudUser = emasRepo.findByPhone(phone);
                if (cloudUser != null) {
                    String cloudHash = cloudUser.getPasswordHash();
                    if (cloudHash != null && !cloudHash.isEmpty() && verifyPassword(password, cloudHash)) {
                        User localUser = ensureLocalUser(cloudUser); if (localUser != null) prefs.edit().putInt(KEY_CURRENT_USER_ID, localUser.getUserId()).apply(); return localUser;
                    }
                    if (cloudHash == null || cloudHash.isEmpty()) {
                        cloudUser.setPasswordHash(generateSaltedHash(password));
                        User localUser = ensureLocalUser(cloudUser); if (localUser != null) prefs.edit().putInt(KEY_CURRENT_USER_ID, localUser.getUserId()).apply(); return localUser;
                    }
                    lastError = "密码错误"; return null;
                }
                lastError = "手机号未注册（本地和云端均未找到）"; return null;
            } catch (Exception e) { lastError = "云端连接失败，请检查网络后重试"; return null; }
        }
        lastError = "手机号未注册"; return null;
    }
    @Override public User findByPhone(String phone) { return database.userDao().getUserByPhone(phone); }
    @Override public void logout() { prefs.edit().remove(KEY_CURRENT_USER_ID).apply(); }
    @Override public User findByShortId(String shortId) { return database.userDao().getUserByShortId(shortId); }
    @Override public User getUserById(int userId) { return database.userDao().getUserById(userId); }
    @Override public void updatePhone(int userId, String phone) { executor.execute(() -> database.userDao().updatePhone(userId, phone, System.currentTimeMillis())); }
    @Override public void updateAvatarUrl(int userId, String avatarUrl) { executor.execute(() -> database.userDao().updateAvatarUrl(userId, avatarUrl, System.currentTimeMillis())); }
    public void shutdown() { executor.shutdown(); }
    @Override public void deleteAccount(int userId) {
        database.runInTransaction(() -> {
            database.harvestLogDao().deleteByUserId(userId); database.wateringLogDao().deleteByUserId(userId);
            database.energyRecordDao().deleteByUserId(userId); database.stepRecordDao().deleteByUserId(userId);
            database.friendDao().deleteByUserId(userId); database.tomatoDao().deleteByUserId(userId);
            database.badgeDao().deleteByUserId(userId); database.userDao().deleteUser(userId);
        }); prefs.edit().remove(KEY_CURRENT_USER_ID).apply();
    }
    @Override public synchronized User ensureLocalUser(User remoteUser) {
        if (remoteUser == null) return null; String shortId = remoteUser.getShortId(); if (shortId == null || shortId.isEmpty()) return null;
        User local = database.userDao().getUserByShortId(shortId);
        String resolvedAvatar = resolveAvatarUrl(remoteUser.getAvatarUrl(), shortId);
        if (local != null) {
            local.setNickname(remoteUser.getNickname()); local.setUserType(remoteUser.getUserType()); local.setAvatarUrl(resolvedAvatar);
            if (remoteUser.getPhone() != null && !remoteUser.getPhone().isEmpty()) local.setPhone(remoteUser.getPhone());
            if (remoteUser.getPasswordHash() != null && !remoteUser.getPasswordHash().isEmpty()) local.setPasswordHash(remoteUser.getPasswordHash());
            local.setUpdatedAt(System.currentTimeMillis()); database.userDao().updateUser(local); return local;
        }
        long now = System.currentTimeMillis(); User newLocal = new User(); newLocal.setNickname(remoteUser.getNickname()); newLocal.setShortId(shortId);
        newLocal.setUserType(remoteUser.getUserType()); newLocal.setAvatarUrl(resolvedAvatar); newLocal.setPhone(remoteUser.getPhone());
        newLocal.setPasswordHash(remoteUser.getPasswordHash()); newLocal.setCreatedAt(now); newLocal.setUpdatedAt(now); newLocal.setDeleted(0);
        database.userDao().insertUser(newLocal); return database.userDao().getUserByShortId(shortId);
    }
    private String resolveAvatarUrl(String avatarUrl, String shortId) {
        if (avatarUrl == null || avatarUrl.isEmpty()) return "";
        if (avatarUrl.startsWith("/data") || avatarUrl.startsWith("/storage")) return avatarUrl;
        if (avatarUrl.length() > 100) { try { byte[] bytes = Base64.decode(avatarUrl, Base64.DEFAULT); File avatarDir = new File(appContext.getFilesDir(), "avatars"); if (!avatarDir.exists()) avatarDir.mkdirs(); File avatarFile = new File(avatarDir, "avatar_" + shortId + ".jpg"); FileOutputStream fos = new FileOutputStream(avatarFile); fos.write(bytes); fos.close(); return avatarFile.getAbsolutePath(); } catch (Exception e) { return avatarUrl; } }
        return avatarUrl;
    }
    @Override public String getTodayDate() { return new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date()); }
    private String generateSaltedHash(String password) { try { SecureRandom random = new SecureRandom(); byte[] salt = new byte[16]; random.nextBytes(salt); String saltHex = bytesToHex(salt); String hashHex = computeSha256(password, salt); return saltHex + ":" + hashHex; } catch (Exception e) { try { return computeSha256(password, "_sporty_tomato_salt_".getBytes("UTF-8")); } catch (Exception e2) { return password; } } }
    private boolean verifyPassword(String password, String storedHash) { if (password == null || storedHash == null) return false; try { int colonIdx = storedHash.indexOf(':'); if (colonIdx > 0) { String saltHex = storedHash.substring(0, colonIdx); String expectedHash = storedHash.substring(colonIdx + 1); String computedHash = computeSha256(password, hexToBytes(saltHex)); return expectedHash.equals(computedHash); } else { String computedHash = computeSha256(password, "_sporty_tomato_salt_".getBytes("UTF-8")); return computedHash.equals(storedHash); } } catch (Exception e) { return false; } }
    private String computeSha256(String password, byte[] salt) { try { MessageDigest md = MessageDigest.getInstance("SHA-256"); md.update(password.getBytes("UTF-8")); md.update(salt); return bytesToHex(md.digest()); } catch (Exception e) { return null; } }
    private static String bytesToHex(byte[] bytes) { StringBuilder hex = new StringBuilder(); for (byte b : bytes) hex.append(String.format("%02x", b & 0xFF)); return hex.toString(); }
    private static byte[] hexToBytes(String hex) { int len = hex.length(); byte[] data = new byte[len / 2]; for (int i = 0; i < len; i += 2) data[i / 2] = (byte) ((Character.digit(hex.charAt(i), 16) << 4) + Character.digit(hex.charAt(i + 1), 16)); return data; }
}