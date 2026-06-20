package com.sportytomato.app.model.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.sportytomato.app.model.entity.Friendship;

import java.util.List;

@Dao
public interface FriendDao {

    /** 插入好友申请 */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    void insert(Friendship friendship);

    /** 更新状态（接受/拒绝/屏蔽） */
    @Update
    void update(Friendship friendship);

    /** 查询某用户的所有好友关系（双向） */
    @Query("SELECT * FROM friendships WHERE (userId = :userId OR friendUserId = :userId) AND status = 1 ORDER BY updatedAt DESC")
    List<Friendship> getAcceptedFriendships(int userId);

    /** 查询发给某用户的待处理申请 */
    @Query("SELECT * FROM friendships WHERE friendUserId = :userId AND status = 0 ORDER BY createdAt DESC")
    List<Friendship> getPendingRequests(int userId);

    /** 查询某用户发出的待处理申请 */
    @Query("SELECT * FROM friendships WHERE userId = :userId AND status = 0 ORDER BY createdAt DESC")
    List<Friendship> getSentRequests(int userId);

    /** 查找两个用户之间的好友关系 */
    @Query("SELECT * FROM friendships WHERE (userId = :a AND friendUserId = :b) OR (userId = :b AND friendUserId = :a) LIMIT 1")
    Friendship findFriendship(int a, int b);

    /** 检查是否已是好友 */
    @Query("SELECT COUNT(*) FROM friendships WHERE ((userId = :a AND friendUserId = :b) OR (userId = :b AND friendUserId = :a)) AND status = 1")
    int areFriends(int a, int b);

    /** 获取收到的待处理申请数 */
    @Query("SELECT COUNT(*) FROM friendships WHERE friendUserId = :userId AND status = 0")
    int getPendingCount(int userId);

    /** 删除好友关系 */
    @Query("DELETE FROM friendships WHERE id = :friendshipId")
    void deleteById(int friendshipId);

    /** 获取好友ID列表 */
    @Query("SELECT CASE WHEN userId = :myId THEN friendUserId ELSE userId END FROM friendships WHERE (userId = :myId OR friendUserId = :myId) AND status = 1")
    List<Integer> getFriendIds(int myId);

    /** 根据ID查找好友关系 */
    @Query("SELECT * FROM friendships WHERE id = :id LIMIT 1")
    Friendship findById(int id);

    /** 获取用户所有好友关系（用于云端同步） */
    @Query("SELECT * FROM friendships WHERE userId = :userId OR friendUserId = :userId")
    List<Friendship> getAllFriendships(int userId);

    /** 注销时删除用户所有好友关系 */
    @Query("DELETE FROM friendships WHERE userId = :userId OR friendUserId = :userId")
    void deleteByUserId(int userId);
}
