package com.sportytomato.app.repository;

import androidx.lifecycle.LiveData;
import com.sportytomato.app.model.entity.User;

public interface UserRepository {
    User getCurrentUser();
    LiveData<User> getCurrentUserLive();
    void createGuestUser(String localId, String nickname);
    void updateNickname(int userId, String nickname);
    User registerByPhone(String phone, String password, String nickname);
    String getLastError();
    User loginByPhone(String phone, String password);
    void logout();
    User findByShortId(String shortId);
    User findByPhone(String phone);
    User getUserById(int userId);
    void updatePhone(int userId, String phone);
    void updateAvatarUrl(int userId, String avatarUrl);
    void deleteAccount(int userId);
    User ensureLocalUser(User remoteUser);
    String getTodayDate();
}