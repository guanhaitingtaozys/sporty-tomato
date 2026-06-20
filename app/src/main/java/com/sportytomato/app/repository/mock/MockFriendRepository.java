package com.sportytomato.app.repository.mock;

import com.sportytomato.app.model.entity.Friendship;
import com.sportytomato.app.model.entity.User;
import com.sportytomato.app.repository.FriendRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class MockFriendRepository implements FriendRepository {

    private final ConcurrentHashMap<Integer, Friendship> store = new ConcurrentHashMap<>();
    private final AtomicInteger nextId = new AtomicInteger(1);
    private final MockUserRepository mockUserRepo;

    public MockFriendRepository(MockUserRepository mockUserRepo) {
        this.mockUserRepo = mockUserRepo;
    }

    @Override
    public Friendship sendRequest(int fromUserId, int toUserId, String message) {
        simulateDelay();
        int id = nextId.getAndIncrement();
        Friendship f = Friendship.createRequest(fromUserId, toUserId, message);
        f.setId(id);
        store.put(id, f);
        return f;
    }

    @Override
    public void acceptRequest(int friendshipId) {
        simulateDelay();
        Friendship f = store.get(friendshipId);
        if (f != null) {
            f.setStatus(1);
            f.setUpdatedAt(System.currentTimeMillis());
        }
    }

    @Override
    public void acceptRequestWithTime(int friendshipId, long updatedAt) {
        simulateDelay();
        Friendship f = store.get(friendshipId);
        if (f != null) {
            f.setStatus(1);
            f.setUpdatedAt(updatedAt);
        }
    }

    @Override
    public void rejectRequest(int friendshipId) {
        simulateDelay();
        Friendship f = store.get(friendshipId);
        if (f != null) {
            f.setStatus(2);
            f.setUpdatedAt(System.currentTimeMillis());
        }
    }

    @Override
    public void deleteFriend(int friendshipId) {
        simulateDelay();
        store.remove(friendshipId);
    }

    @Override
    public List<Friendship> getAcceptedFriendships(int userId) {
        simulateDelay();
        List<Friendship> result = new ArrayList<>();
        for (Friendship f : store.values()) {
            if (f.getStatus() == 1 && (f.getUserId() == userId || f.getFriendUserId() == userId)) {
                result.add(f);
            }
        }
        return result;
    }

    @Override
    public List<Friendship> getPendingRequests(int userId) {
        simulateDelay();
        List<Friendship> result = new ArrayList<>();
        for (Friendship f : store.values()) {
            if (f.getStatus() == 0 && f.getFriendUserId() == userId) {
                result.add(f);
            }
        }
        return result;
    }

    @Override
    public int getPendingCount(int userId) {
        simulateDelay();
        int count = 0;
        for (Friendship f : store.values()) {
            if (f.getStatus() == 0 && f.getFriendUserId() == userId) {
                count++;
            }
        }
        return count;
    }

    @Override
    public boolean areFriends(int a, int b) {
        simulateDelay();
        for (Friendship f : store.values()) {
            if (f.getStatus() == 1
                    && ((f.getUserId() == a && f.getFriendUserId() == b)
                    || (f.getUserId() == b && f.getFriendUserId() == a))) {
                return true;
            }
        }
        return false;
    }

    @Override
    public Friendship findFriendship(int userIdA, int userIdB) {
        simulateDelay();
        for (Friendship f : store.values()) {
            if ((f.getUserId() == userIdA && f.getFriendUserId() == userIdB)
                    || (f.getUserId() == userIdB && f.getFriendUserId() == userIdA)) {
                return f;
            }
        }
        return null;
    }

    @Override
    public List<Integer> getFriendIds(int userId) {
        simulateDelay();
        List<Integer> ids = new ArrayList<>();
        for (Friendship f : store.values()) {
            if (f.getStatus() == 1) {
                if (f.getUserId() == userId) {
                    ids.add(f.getFriendUserId());
                } else if (f.getFriendUserId() == userId) {
                    ids.add(f.getUserId());
                }
            }
        }
        return ids;
    }

    @Override
    public List<User> getFriendUsers(int userId) {
        simulateDelay();
        List<Integer> ids = getFriendIds(userId);
        List<User> users = new ArrayList<>();
        for (int id : ids) {
            User user = mockUserRepo.getUserById(id);
            if (user != null) {
                users.add(user);
            }
        }
        return users;
    }

    private void simulateDelay() {
        try {
            Thread.sleep(300);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
