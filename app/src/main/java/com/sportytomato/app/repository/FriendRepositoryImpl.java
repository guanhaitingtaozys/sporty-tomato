package com.sportytomato.app.repository;

import com.sportytomato.app.model.database.AppDatabase;
import com.sportytomato.app.model.entity.Friendship;
import com.sportytomato.app.model.entity.User;
import java.util.ArrayList;
import java.util.List;

public class FriendRepositoryImpl implements FriendRepository {
    private final AppDatabase database;
    public FriendRepositoryImpl(AppDatabase database) { this.database = database; }

    @Override public Friendship sendRequest(int fromUserId, int toUserId, String message) {
        Friendship existing = database.friendDao().findFriendship(fromUserId, toUserId);
        if (existing != null) return existing;
        Friendship f = Friendship.createRequest(fromUserId, toUserId, message);
        database.friendDao().insert(f);
        return database.friendDao().findFriendship(fromUserId, toUserId);
    }
    @Override public void acceptRequest(int friendshipId) { Friendship f = database.friendDao().findById(friendshipId); if (f != null && f.getStatus() == 0) { f.setStatus(1); f.setUpdatedAt(System.currentTimeMillis()); database.friendDao().update(f); } }
    @Override public void acceptRequestWithTime(int friendshipId, long updatedAt) { Friendship f = database.friendDao().findById(friendshipId); if (f != null && f.getStatus() == 0) { f.setStatus(1); f.setUpdatedAt(updatedAt); database.friendDao().update(f); } }
    @Override public void rejectRequest(int friendshipId) { Friendship f = database.friendDao().findById(friendshipId); if (f != null && f.getStatus() == 0) { f.setStatus(2); f.setUpdatedAt(System.currentTimeMillis()); database.friendDao().update(f); } }
    @Override public void deleteFriend(int friendshipId) { database.friendDao().deleteById(friendshipId); }
    @Override public List<Friendship> getAcceptedFriendships(int userId) { return database.friendDao().getAcceptedFriendships(userId); }
    @Override public List<Friendship> getPendingRequests(int userId) { return database.friendDao().getPendingRequests(userId); }
    @Override public int getPendingCount(int userId) { return database.friendDao().getPendingCount(userId); }
    @Override public boolean areFriends(int a, int b) { return database.friendDao().areFriends(a, b) > 0; }
    @Override public List<Integer> getFriendIds(int userId) { return database.friendDao().getFriendIds(userId); }
    @Override public List<User> getFriendUsers(int userId) { List<Integer> ids = database.friendDao().getFriendIds(userId); List<User> users = new ArrayList<>(); for (int id : ids) { User u = database.userDao().getUserById(id); if (u != null) users.add(u); } return users; }
    @Override public Friendship findFriendship(int userIdA, int userIdB) { return database.friendDao().findFriendship(userIdA, userIdB); }
}