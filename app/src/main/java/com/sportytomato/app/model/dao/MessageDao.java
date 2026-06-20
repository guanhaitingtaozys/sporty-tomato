package com.sportytomato.app.model.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import com.sportytomato.app.model.entity.Message;

import java.util.List;

@Dao
public interface MessageDao {

    @Insert
    void insert(Message message);

    /** 获取两人之间的会话记录（双向，按时间升序） */
    @Query("SELECT * FROM messages WHERE (fromUserId = :a AND toUserId = :b) OR (fromUserId = :b AND toUserId = :a) ORDER BY timestamp ASC")
    List<Message> getConversation(int a, int b);

    /** 分页获取会话记录（按时间降序，最新在前，用于上拉加载更多） */
    @Query("SELECT * FROM messages WHERE (fromUserId = :user1 AND toUserId = :user2) OR (fromUserId = :user2 AND toUserId = :user1) ORDER BY timestamp DESC LIMIT :limit OFFSET :offset")
    List<Message> getConversationPaged(int user1, int user2, int limit, int offset);

    /** 获取发给某用户的未读消息 */
    @Query("SELECT * FROM messages WHERE toUserId = :userId AND read = 0 ORDER BY timestamp DESC")
    List<Message> getUnreadMessages(int userId);

    /** 获取某个好友发给当前用户的未读消息计数 */
    @Query("SELECT COUNT(*) FROM messages WHERE fromUserId = :fromUserId AND toUserId = :toUserId AND read = 0")
    int getUnreadCount(int fromUserId, int toUserId);

    /** 标记两人之间来自对方的未读消息为已读 */
    @Query("UPDATE messages SET read = 1 WHERE fromUserId = :from AND toUserId = :to AND read = 0")
    void markRead(int from, int to);

    /** 获取与某用户相关的最新一条消息 */
    @Query("SELECT * FROM messages WHERE (fromUserId = :userIdA AND toUserId = :userIdB) OR (fromUserId = :userIdB AND toUserId = :userIdA) ORDER BY timestamp DESC LIMIT 1")
    Message getLatestMessage(int userIdA, int userIdB);

    /** 注销时删除用户所有相关消息 */
    @Query("DELETE FROM messages WHERE fromUserId = :userId OR toUserId = :userId")
    void deleteByUserId(int userId);
}
