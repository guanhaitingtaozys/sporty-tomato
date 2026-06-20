package com.sportytomato.app.service;

import com.sportytomato.app.model.entity.Friendship;
import com.sportytomato.app.model.entity.User;
import com.sportytomato.app.repository.FriendRepository;
import com.sportytomato.app.repository.UserRepository;
import com.sportytomato.app.repository.emas.EMASUserRepository;

public class FriendService {
    private final FriendRepository friendRepo;
    private final UserRepository userRepo;
    public FriendService(FriendRepository friendRepo, UserRepository userRepo) { this.friendRepo = friendRepo; this.userRepo = userRepo; }

    public FriendResult sendRequest(int fromUserId, String targetShortId, String message) {
        if (targetShortId == null || targetShortId.trim().isEmpty()) return FriendResult.failure("请输入短ID");
        User target = userRepo.findByShortId(targetShortId.trim());
        if (target == null && EMASClient.getInstance().isInitialized()) {
            EMASUserRepository emasRepo = EMASUserRepository.getInstance();
            target = emasRepo.findByShortId(targetShortId.trim());
            if (target == null) target = emasRepo.findByPhone(targetShortId.trim());
            if (target != null) target = userRepo.ensureLocalUser(target);
        }
        if (target == null) return FriendResult.failure("未找到该用户");
        if (target.getUserId() == fromUserId) return FriendResult.failure("不能添加自己为好友");
        if (friendRepo.areFriends(fromUserId, target.getUserId())) return FriendResult.failure("已经是好友了");
        Friendship sent = friendRepo.sendRequest(fromUserId, target.getUserId(), message != null ? message : "");
        if (sent != null) return FriendResult.success("好友申请已发送");
        return FriendResult.failure("发送失败，可能已经申请过了");
    }

    public static class FriendResult {
        private final boolean success; private final String message;
        private FriendResult(boolean success, String message) { this.success = success; this.message = message; }
        public static FriendResult success(String message) { return new FriendResult(true, message); }
        public static FriendResult failure(String message) { return new FriendResult(false, message); }
        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
    }
}