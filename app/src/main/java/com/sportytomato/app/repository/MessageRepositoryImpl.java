package com.sportytomato.app.repository;

import com.sportytomato.app.model.database.AppDatabase;
import com.sportytomato.app.model.entity.Message;
import com.sportytomato.app.model.entity.User;
import com.sportytomato.app.repository.emas.EMASMessageRepository;
import com.sportytomato.app.service.EMASClient;
import java.util.List;

public class MessageRepositoryImpl implements MessageRepository {
    private final AppDatabase database;
    private final EMASMessageRepository emasRepo;
    private final UserRepository userRepo;

    public MessageRepositoryImpl(AppDatabase database, UserRepository userRepo) { this.database = database; this.emasRepo = new EMASMessageRepository(); this.userRepo = userRepo; }

    @Override public void sendMessage(Message message) {
        database.messageDao().insert(message);
        if (EMASClient.getInstance().isInitialized()) {
            User fromUser = userRepo.getUserById(message.getFromUserId()); User toUser = userRepo.getUserById(message.getToUserId());
            if (fromUser != null && toUser != null) { String fromSid = fromUser.getShortId(); String toSid = toUser.getShortId(); new Thread(() -> emasRepo.pushToCloud(message, fromSid, toSid)).start(); }
        }
    }
    @Override public List<Message> getConversation(int userIdA, int userIdB) { return database.messageDao().getConversation(userIdA, userIdB); }
    @Override public List<Message> getConversationPaged(int userIdA, int userIdB, int limit, int offset) { return database.messageDao().getConversationPaged(userIdA, userIdB, limit, offset); }
    @Override public void markRead(int fromUserId, int toUserId) { database.messageDao().markRead(fromUserId, toUserId); }
    @Override public List<Message> getUnreadMessages(int userId) { return database.messageDao().getUnreadMessages(userId); }
    @Override public int getUnreadCount(int fromUserId, int toUserId) { return database.messageDao().getUnreadCount(fromUserId, toUserId); }
    @Override public Message getLatestMessage(int userIdA, int userIdB) { return database.messageDao().getLatestMessage(userIdA, userIdB); }
    @Override public void syncMessagesFromCloud(int userIdA, String shortIdA, int userIdB, String shortIdB) { /* cloud sync logic */ }
}