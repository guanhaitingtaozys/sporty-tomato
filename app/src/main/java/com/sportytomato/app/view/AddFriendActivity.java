package com.sportytomato.app.view;

import android.app.AlertDialog;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.sportytomato.app.databinding.ActivityAddFriendBinding;
import com.sportytomato.app.model.entity.Friendship;
import com.sportytomato.app.model.entity.User;
import com.sportytomato.app.repository.FriendRepository;
import com.sportytomato.app.repository.UserRepository;
import com.sportytomato.app.repository.emas.EMASUserRepository;
import com.sportytomato.app.service.EMASClient;
import com.sportytomato.app.service.FriendService;
import com.sportytomato.app.service.RepositoryManager;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * 添加好友页面 — 搜索用户 + 发送好友申请
 * 两种模式：
 *   - 携带 target_user_id：直接展示用户信息
 *   - 无 target_user_id：展示搜索栏，用户输入短ID搜索
 */
public class AddFriendActivity extends AppCompatActivity {

    private ActivityAddFriendBinding binding;
    private FriendService friendService;
    private FriendRepository friendRepo;
    private UserRepository userRepo;
    private User targetUser;
    private User currentUser;
    private int targetUserId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAddFriendBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        targetUserId = getIntent().getIntExtra("target_user_id", -1);

        RepositoryManager rm = RepositoryManager.getInstance();
        friendRepo = rm.getFriendRepo();
        userRepo = rm.getUserRepo();
        friendService = new FriendService(friendRepo, userRepo);

        // 后台加载用户数据
        final int tid = targetUserId;
        new Thread(() -> {
            currentUser = userRepo.getCurrentUser();

            runOnUiThread(() -> {
                if (isFinishing() || isDestroyed()) return;
                if (currentUser == null) {
                    Toast.makeText(AddFriendActivity.this, "请先登录", Toast.LENGTH_SHORT).show();
                    finish();
                    return;
                }

                if (tid > 0) {
                    new Thread(() -> {
                        targetUser = userRepo.getUserById(tid);
                        runOnUiThread(() -> {
                            if (isFinishing() || isDestroyed()) return;
                            if (targetUser != null) {
                                showTargetUser();
                            } else {
                                Toast.makeText(AddFriendActivity.this, "目标用户不存在", Toast.LENGTH_SHORT).show();
                                targetUserId = -1;
                                setupSearchMode();
                            }
                            setupSendButton();
                        });
                    }).start();
                } else {
                    setupSearchMode();
                    setupSendButton();
                }
            });
        }).start();
    }

    /**
     * 搜索模式：用户输入短ID或手机号来查找好友
     */
    private void setupSearchMode() {
        binding.llSearchArea.setVisibility(View.VISIBLE);
        binding.tvSearchHint.setVisibility(View.VISIBLE);

        binding.btnSearch.setOnClickListener(v -> {
            String input = binding.etSearchId.getText().toString().trim();
            if (input.isEmpty()) {
                Toast.makeText(this, "请输入好友短ID或手机号", Toast.LENGTH_SHORT).show();
                return;
            }

            final String searchInput = input;
            new Thread(() -> {
                User found = null;

                // 本地搜索
                if (searchInput.matches("^\\d{6}$")) {
                    found = userRepo.findByShortId(searchInput);
                } else if (searchInput.matches("^1\\d{10}$")) {
                    found = userRepo.findByPhone(searchInput);
                }
                if (found == null) found = userRepo.findByShortId(searchInput);
                if (found == null) found = userRepo.findByPhone(searchInput);

                if (found != null) {
                    final User finalFound = found;
                    runOnUiThread(() -> {
                        if (isFinishing() || isDestroyed()) return;
                        handleSearchResult(finalFound);
                    });
                    return;
                }

                // 云端搜索
                if (!EMASClient.getInstance().isInitialized()) {
                    runOnUiThread(() -> Toast.makeText(AddFriendActivity.this, "未找到该用户", Toast.LENGTH_SHORT).show());
                    return;
                }

                runOnUiThread(() -> Toast.makeText(AddFriendActivity.this, "正在云端搜索...", Toast.LENGTH_SHORT).show());

                User cloudFound = null;
                EMASClient client = EMASClient.getInstance();
                try {
                    JSONObject query = new JSONObject();
                    query.put("shortId", searchInput);
                    JSONObject options = new JSONObject();
                    options.put("limit", 1);
                    String params = EMASClient.buildDbParamsWithOptions("find", "users",
                            null, query.toString(), options.toString());
                    String result = client.callFunction("sporty-api", params);

                    if (result != null) {
                        JSONObject json = new JSONObject(result);
                        if (json.optBoolean("success") && json.optInt("affectedDocs", 0) > 0) {
                            JSONArray arr = json.optJSONArray("result");
                            if (arr != null && arr.length() > 0) {
                                cloudFound = EMASUserRepository.getInstance().parseUserJson(arr.getJSONObject(0));
                            }
                        }
                    }
                } catch (Exception e) {
                    android.util.Log.e("AddFriend", "Cloud search error: " + e.getMessage());
                }

                final User finalFound2 = cloudFound;
                runOnUiThread(() -> {
                    if (isFinishing() || isDestroyed()) return;
                    if (finalFound2 != null) {
                        User synced = userRepo.ensureLocalUser(finalFound2);
                        if (synced != null) {
                            handleSearchResult(synced);
                        } else {
                            Toast.makeText(AddFriendActivity.this,
                                    "未找到该用户", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(AddFriendActivity.this,
                                "未找到该用户（已搜索本地和云端）", Toast.LENGTH_SHORT).show();
                    }
                });
            }).start();
        });
    }

    private void handleSearchResult(User found) {
        if (found.getUserId() == currentUser.getUserId()) {
            Toast.makeText(this, "不能添加自己为好友", Toast.LENGTH_SHORT).show();
            return;
        }
        targetUser = found;
        targetUserId = found.getUserId();
        showTargetUser();
    }

    /**
     * 展示目标用户信息
     */
    private void showTargetUser() {
        binding.llSearchArea.setVisibility(View.GONE);
        binding.tvSearchHint.setVisibility(View.GONE);
        binding.llUserInfo.setVisibility(View.VISIBLE);

        binding.tvTargetNickname.setText(targetUser.getNickname());
        binding.tvTargetShortId.setText("ID: " + targetUser.getShortId());

        // 加载目标用户头像
        String avatarUrl = targetUser.getAvatarUrl();
        if (avatarUrl != null && !avatarUrl.isEmpty()) {
            try {
                android.graphics.Bitmap bmp = android.graphics.BitmapFactory.decodeFile(avatarUrl);
                if (bmp != null) {
                    ((android.widget.ImageView) binding.ivAvatarPlaceholder).setImageBitmap(bmp);
                }
            } catch (Exception ignored) {}
        }
    }

    private void setupSendButton() {
        binding.etVerifyMessage.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                binding.tvMessageHint.setText(s.length() + "/30");
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        binding.btnSendRequest.setOnClickListener(v -> {
            if (targetUser == null) {
                Toast.makeText(this, "请先搜索好友", Toast.LENGTH_SHORT).show();
                return;
            }
            final String message = binding.etVerifyMessage.getText().toString().trim();
            final String targetShortId = targetUser.getShortId().trim();

            // 后台查找用户并发送请求
            new Thread(() -> {
                User localTarget = userRepo.findByShortId(targetShortId);

                if (localTarget == null && EMASClient.getInstance().isInitialized()) {
                    try {
                        com.sportytomato.app.repository.emas.EMASUserRepository emasRepo =
                                com.sportytomato.app.repository.emas.EMASUserRepository.getInstance();
                        User cloudTarget = emasRepo.findByShortId(targetShortId);
                        if (cloudTarget == null) {
                            cloudTarget = emasRepo.findByPhone(targetShortId);
                        }
                        if (cloudTarget != null) {
                            cloudTarget = userRepo.ensureLocalUser(cloudTarget);
                        }
                        localTarget = cloudTarget;
                    } catch (Exception e) {
                        android.util.Log.e("AddFriend", "Cloud search error: " + e.getMessage());
                    }
                }

                final User finalTarget = localTarget;
                runOnUiThread(() -> {
                    if (isFinishing() || isDestroyed()) return;
                    doSendRequest(finalTarget, message);
                });
            }).start();
        });
    }

    private void doSendRequest(User target, String message) {
        if (target == null) {
            binding.tvResult.setVisibility(View.VISIBLE);
            binding.tvResult.setText("未找到该用户");
            binding.tvResult.setTextColor(getResources().getColor(com.sportytomato.app.R.color.error));
            return;
        }
        if (target.getUserId() == currentUser.getUserId()) {
            binding.tvResult.setVisibility(View.VISIBLE);
            binding.tvResult.setText("不能添加自己为好友");
            binding.tvResult.setTextColor(getResources().getColor(com.sportytomato.app.R.color.error));
            return;
        }

        final int targetUid = target.getUserId();
        final String targetSid = target.getShortId();
        new Thread(() -> {
            if (friendRepo.areFriends(currentUser.getUserId(), targetUid)) {
                runOnUiThread(() -> {
                    binding.tvResult.setVisibility(View.VISIBLE);
                    binding.tvResult.setText("已经是好友了");
                    binding.tvResult.setTextColor(getResources().getColor(com.sportytomato.app.R.color.error));
                });
                return;
            }

            try {
                Friendship sent = friendRepo.sendRequest(currentUser.getUserId(), targetUid,
                        message != null ? message : "");
                runOnUiThread(() -> {
                    if (sent != null) {
                        binding.btnSendRequest.setEnabled(false);
                        binding.btnSendRequest.setText("申请已发送");
                        binding.etVerifyMessage.setEnabled(false);
                        binding.tvResult.setVisibility(View.VISIBLE);
                        binding.tvResult.setText("好友申请已发送");
                        binding.tvResult.setTextColor(getResources().getColor(com.sportytomato.app.R.color.primary));
                        pushToCloud(targetSid, message);
                    } else {
                        binding.tvResult.setVisibility(View.VISIBLE);
                        binding.tvResult.setText("发送失败");
                        binding.tvResult.setTextColor(getResources().getColor(com.sportytomato.app.R.color.error));
                    }
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    binding.tvResult.setVisibility(View.VISIBLE);
                    binding.tvResult.setText("异常: " + e.getMessage());
                    binding.tvResult.setTextColor(getResources().getColor(com.sportytomato.app.R.color.error));
                });
            }
        }).start();
    }

    private void pushToCloud(String toShortId, String message) {
        if (!EMASClient.getInstance().isInitialized() || currentUser == null) return;
        final String msg = message;
        new Thread(() -> {
            try {
                JSONObject cloudData = new JSONObject();
                cloudData.put("fromShortId", currentUser.getShortId());
                cloudData.put("toShortId", toShortId);
                cloudData.put("status", 0);
                cloudData.put("verifyMessage", msg != null ? msg : "");
                cloudData.put("createdAt", System.currentTimeMillis());
                cloudData.put("updatedAt", System.currentTimeMillis());
                String params = EMASClient.buildDbParams("insertOne", "friendships",
                        cloudData.toString(), null);
                EMASClient.getInstance().callFunction("sporty-api", params);
                android.util.Log.i("AddFriend", "云端推送好友申请完成");
            } catch (Exception ex) {
                android.util.Log.e("AddFriend", "云端推送异常: " + ex.getMessage());
            }
        }).start();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}
