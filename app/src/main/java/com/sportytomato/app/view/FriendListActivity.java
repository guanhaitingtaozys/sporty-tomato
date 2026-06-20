package com.sportytomato.app.view;

import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.sportytomato.app.databinding.ActivityFriendListBinding;
import com.sportytomato.app.model.entity.Friendship;
import com.sportytomato.app.model.entity.User;
import com.sportytomato.app.repository.FriendRepository;
import com.sportytomato.app.repository.UserRepository;
import com.sportytomato.app.service.RepositoryManager;
import com.sportytomato.app.util.DialogHelper;
import com.sportytomato.app.view.adapter.FriendAdapter;

import java.util.ArrayList;
import java.util.List;

/**
 * 好友列表 Activity — RecyclerView + SwipeRefresh + 搜索过滤
 */
public class FriendListActivity extends AppCompatActivity {

    private ActivityFriendListBinding binding;
    private FriendAdapter adapter;
    private FriendRepository friendRepo;
    private UserRepository userRepo;
    private User currentUser;
    private List<Friendship> allFriendships;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityFriendListBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        RepositoryManager rm = RepositoryManager.getInstance();
        friendRepo = rm.getFriendRepo();
        userRepo = rm.getUserRepo();

        setupRecyclerView();
        setupSwipeRefresh();
        setupFilter();

        // 后台加载好友数据
        new Thread(() -> {
            currentUser = userRepo.getCurrentUser();
            if (currentUser != null) {
                runOnUiThread(this::loadFriends);
            }
        }).start();
    }

    private void setupRecyclerView() {
        adapter = new FriendAdapter();
        binding.rvFriends.setLayoutManager(new LinearLayoutManager(this));
        binding.rvFriends.setAdapter(adapter);

        adapter.setOnFriendClickListener((friendshipId, friendUserId) -> {
            // Show friend detail
            User friend = userRepo.getUserById(friendUserId);
            if (friend != null) {
                DialogHelper.showStyledDialog(this,
                        friend.getNickname(),
                        "短ID: " + friend.getShortId() + "\n好友ID: " + friendshipId,
                        "确定", null);
            }
        });

        adapter.setOnFriendLongClickListener(friendshipId -> {
            DialogHelper.showStyledConfirmDialog(this,
                    "删除好友", "确定要删除该好友吗？",
                    "删除", (d, w) -> {
                        friendRepo.deleteFriend(friendshipId);
                        loadFriends();
                    },
                    "取消", null);
        });
    }

    private void setupSwipeRefresh() {
        binding.swipeRefresh.setOnRefreshListener(this::loadFriends);
    }

    private void setupFilter() {
        binding.etFilter.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                adapter.filter(s.toString());
            }

            @Override
            public void afterTextChanged(android.text.Editable s) {}
        });
    }

    private void loadFriends() {
        if (currentUser == null) return;

        binding.swipeRefresh.setRefreshing(true);
        new Thread(() -> {
            List<Friendship> friendships = friendRepo.getAcceptedFriendships(currentUser.getUserId());
            List<User> users = friendRepo.getFriendUsers(currentUser.getUserId());
            final int uid = currentUser.getUserId();
            runOnUiThread(() -> {
                if (isFinishing() || isDestroyed()) return;
                adapter.setFriendData(friendships, users, uid);
                binding.swipeRefresh.setRefreshing(false);
            });
        }).start();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}
