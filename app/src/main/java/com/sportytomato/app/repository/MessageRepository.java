package com.sportytomato.app.repository;

import com.sportytomato.app.model.entity.Message;
import java.util.List;

public interface MessageRepository {
    void sendMessage(Message message);
    List<Message> getConversation(int userIdA, int userIdB);
    List<Message> getConversationPaged(int userIdA, int userIdB, int limit, int offset);
    void markRead(int fromUserId, int toUserId);
    List<Message> getUnreadMessages(int userId);
    Message getLatestMessage(int userIdA, int userIdB);
    void syncMessagesFromCloud(int userIdA, String shortIdA, int userIdB, String shortIdB);
    int getUnreadCount(int fromUserId, int toUserId);
}