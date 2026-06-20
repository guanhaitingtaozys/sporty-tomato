package com.sportytomato.app.repository;

import com.sportytomato.app.model.entity.Friendship;
import com.sportytomato.app.model.entity.User;
import java.util.List;

public interface FriendRepository {
    Friendship sendRequest(int fromUserId, int toUserId, String message);
    void acceptRequest(int friendshipId);
    void acceptRequestWithTime(int friendshipId, long updatedAt);
    void rejectRequest(int friendshipId);
    void deleteFriend(int friendshipId);
    List<Friendship> getAcceptedFriendships(int userId);
    List<Friendship> getPendingRequests(int userId);
    int getPendingCount(int userId);
    boolean areFriends(int a, int b);
    List<Integer> getFriendIds(int userId);
    List<User> getFriendUsers(int userId);
    Friendship findFriendship(int userIdA, int userIdB);
}