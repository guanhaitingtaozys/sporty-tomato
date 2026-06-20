package com.sportytomato.app.model.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.sportytomato.app.model.entity.User;

@Dao
public interface UserDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    long insertUser(User user);

    @Update
    void updateUser(User user);

    @Query("SELECT * FROM users WHERE localId = :localId LIMIT 1")
    User getUserByLocalId(String localId);

    @Query("SELECT * FROM users WHERE userId = :userId LIMIT 1")
    User getUserById(int userId);

    @Query("SELECT * FROM users WHERE userId = :userId LIMIT 1")
    LiveData<User> getUserByIdLive(int userId);

    @Query("UPDATE users SET nickname = :nickname, updatedAt = :updatedAt WHERE userId = :userId")
    void updateNickname(int userId, String nickname, long updatedAt);

    /** 根据手机号查找用户 */
    @Query("SELECT * FROM users WHERE phone = :phone LIMIT 1")
    User getUserByPhone(String phone);

    /** 根据短 ID 搜索用户 */
    @Query("SELECT * FROM users WHERE shortId = :shortId LIMIT 1")
    User getUserByShortId(String shortId);

    /** 获取所有用户 ID 列表（用于每日重置等批量操作） */
    @Query("SELECT userId FROM users")
    java.util.List<Integer> getAllUserIds();

    /** 更新用户手机号 */
    @Query("UPDATE users SET phone = :phone, updatedAt = :updatedAt WHERE userId = :userId")
    void updatePhone(int userId, String phone, long updatedAt);

    /** 更新用户头像 */
    @Query("UPDATE users SET avatarUrl = :avatarUrl, updatedAt = :updatedAt WHERE userId = :userId")
    void updateAvatarUrl(int userId, String avatarUrl, long updatedAt);

    /** 注销账号：软删除标记 */
    @Query("UPDATE users SET deleted = 1, phone = NULL, passwordHash = NULL, updatedAt = :updatedAt WHERE userId = :userId")
    void softDeleteUser(int userId, long updatedAt);

    /** 注销账号：硬删除 */
    @Query("DELETE FROM users WHERE userId = :userId")
    void deleteUser(int userId);

    /** 批量获取用户（用于好友列表一次性加载，避免N+1查询） */
    @Query("SELECT * FROM users WHERE userId IN (:userIds)")
    java.util.List<User> getUsersByIds(java.util.List<Integer> userIds);
}
