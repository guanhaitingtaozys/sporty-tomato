package com.sportytomato.app.view;

import android.content.res.ColorStateList;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.sportytomato.app.databinding.ActivityRankBinding;
import com.sportytomato.app.model.entity.HarvestLog;
import com.sportytomato.app.model.entity.User;
import com.sportytomato.app.repository.FriendRepository;
import com.sportytomato.app.repository.HarvestRepository;
import com.sportytomato.app.repository.UserRepository;
import com.sportytomato.app.repository.emas.EMASUserRepository;
import com.sportytomato.app.service.EMASClient;
import com.sportytomato.app.service.RankService;
import com.sportytomato.app.service.RepositoryManager;
import com.sportytomato.app.view.adapter.RankAdapter;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Calendar;
import java.util.List;

/**
 * 排行榜 Activity — 日榜/周榜/月榜
 */
public class RankActivity extends AppCompatActivity {

    private ActivityRankBinding binding;
    private RankService rankService;
    private RankAdapter adapter;
    private UserRepository userRepo;
    private int currentMode = 0; // 0=日榜, 1=周榜, 2=月榜
    private android.os.Handler autoRefreshHandler;
    private static final long AUTO_REFRESH_MS = 30_000; // 30秒自动刷新

    private final Runnable autoRefreshRunnable = new Runnable() {
        @Override
        public void run() {
            loadRanking();
            if (autoRefreshHandler != null) {
                autoRefreshHandler.postDelayed(this, AUTO_REFRESH_MS);
            }
        }
    };

    private static final int MODE_DAILY = 0;
    private static final int MODE_WEEKLY = 1;
    private static final int MODE_MONTHLY = 2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityRankBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        RepositoryManager rm = RepositoryManager.getInstance();
        FriendRepository friendRepo = rm.getFriendRepo();
        HarvestRepository harvestRepo = rm.getHarvestRepo();
        userRepo = rm.getUserRepo();
        rankService = new RankService(friendRepo, harvestRepo, userRepo);

        binding.toolbar.setNavigationOnClickListener(v -> finish());

        setupRecyclerView();
        setupToggle();
        setupSwipeRefresh();
        loadRanking();

        // 启动30秒自动轮询
        autoRefreshHandler = new android.os.Handler();
        autoRefreshHandler.postDelayed(autoRefreshRunnable, AUTO_REFRESH_MS);
    }

    private void setupRecyclerView() {
        adapter = new RankAdapter();
        binding.rvRank.setLayoutManager(new LinearLayoutManager(this));
        binding.rvRank.setAdapter(adapter);
    }

    private void setupToggle() {
        binding.btnDaily.setOnClickListener(v -> {
            currentMode = MODE_DAILY;
            updateToggleStyle();
            loadRanking();
        });

        binding.btnWeekly.setOnClickListener(v -> {
            currentMode = MODE_WEEKLY;
            updateToggleStyle();
            loadRanking();
        });

        binding.btnMonthly.setOnClickListener(v -> {
            currentMode = MODE_MONTHLY;
            updateToggleStyle();
            loadRanking();
        });

        updateToggleStyle();
    }

    private void setupSwipeRefresh() {
        binding.swipeRefresh.setOnRefreshListener(() -> {
            loadRanking();
            binding.swipeRefresh.postDelayed(() -> {
                if (binding != null) binding.swipeRefresh.setRefreshing(false);
            }, 1500);
        });
    }

    private void updateToggleStyle() {
        int primaryColor = getResources().getColor(com.sportytomato.app.R.color.primary);
        int variantColor = getResources().getColor(com.sportytomato.app.R.color.on_surface_variant);

        binding.btnDaily.setBackgroundTintList(ColorStateList.valueOf(
                currentMode == MODE_DAILY ? primaryColor : variantColor));
        binding.btnWeekly.setBackgroundTintList(ColorStateList.valueOf(
                currentMode == MODE_WEEKLY ? primaryColor : variantColor));
        binding.btnMonthly.setBackgroundTintList(ColorStateList.valueOf(
                currentMode == MODE_MONTHLY ? primaryColor : variantColor));
    }

    private void loadRanking() {
        // 后台检查用户
        new Thread(() -> {
            User currentUser = userRepo.getCurrentUser();
            if (currentUser == null) {
                runOnUiThread(() -> Toast.makeText(RankActivity.this, "请先登录", Toast.LENGTH_SHORT).show());
                return;
            }

            // 先尝试从云端同步好友的收获数据
            if (EMASClient.getInstance().isInitialized()) {
                final long[] range = getTimeRangeForMode();
                final List<Integer> friendIds = RepositoryManager.getInstance().getFriendRepo()
                        .getFriendIds(currentUser.getUserId());
                final int userId = currentUser.getUserId();
                android.util.Log.i("RankAct", "刷新排行榜: userId=" + userId + " friendCount=" + friendIds.size()
                        + " range=" + range[0] + "-" + range[1]);

                syncHarvestLogsFromCloud(friendIds, range[0], range[1]);
                runOnUiThread(() -> {
                    if (isFinishing() || isDestroyed()) return;
                    doLoadRanking(userId);
                });
            } else {
                android.util.Log.i("RankAct", "EMAS未初始化，直接加载本地排行");
                runOnUiThread(() -> {
                    if (isFinishing() || isDestroyed()) return;
                    doLoadRanking(currentUser.getUserId());
                });
            }
        }).start();
    }

    private void doLoadRanking(int userId) {
        new Thread(() -> {
            List<RankService.RankEntry> entries;
            switch (currentMode) {
                case MODE_DAILY:
                    entries = rankService.getDailyRanking(userId);
                    break;
                case MODE_WEEKLY:
                    entries = rankService.getWeeklyRanking(userId);
                    break;
                case MODE_MONTHLY:
                default:
                    entries = rankService.getMonthlyRanking(userId);
                    break;
            }
            final List<RankService.RankEntry> finalEntries = entries;
            runOnUiThread(() -> {
                if (isFinishing() || isDestroyed()) return;
                adapter.setData(finalEntries, userId);
                Toast.makeText(RankActivity.this, "排行榜已更新", Toast.LENGTH_SHORT).show();
            });
        }).start();
    }

    /**
     * 从 EMAS 云端拉取好友的 harvest_logs 并同步到本地 Room
     */
    private void syncHarvestLogsFromCloud(List<Integer> friendIds, long startTime, long endTime) {
        UserRepository ur = RepositoryManager.getInstance().getUserRepo();
        HarvestRepository hr = RepositoryManager.getInstance().getHarvestRepo();

        for (int fid : friendIds) {
            User friend = ur.getUserById(fid);
            if (friend == null) continue;
            String shortId = friend.getShortId();
            if (shortId == null || shortId.isEmpty()) continue;

            try {
                JSONObject query = new JSONObject();
                query.put("shortId", shortId);
                JSONObject timeQuery = new JSONObject();
                timeQuery.put("$gte", startTime);
                timeQuery.put("$lte", endTime);
                query.put("harvestedAt", timeQuery);

                String params = EMASClient.buildDbParamsWithOptions("find", "harvest_logs",
                        null, query.toString(), null);
                String result = EMASClient.getInstance().callFunction("sporty-api", params);
                if (result == null) continue;

                JSONObject json = new JSONObject(result);
                if (!json.optBoolean("success")) continue;

                JSONArray arr = json.optJSONArray("result");
                if (arr == null || arr.length() == 0) continue;

                for (int i = 0; i < arr.length(); i++) {
                    JSONObject log = arr.getJSONObject(i);
                    String requestId = log.optString("_id", "");
                    long harvestedAt = log.optLong("harvestedAt", 0);

                    // 检查是否已存在（用requestId去重）
                    if (hr.getByRequestId(requestId) != null) continue;

                    // 插入本地
                    HarvestLog h = new HarvestLog();
                    h.setUserId(fid);
                    h.setHarvestedAt(harvestedAt);
                    h.setRequestId(requestId);
                    h.setTomatoId(0); // 排行榜不看具体番茄
                    hr.insertHarvestLog(h);
                }
            } catch (Exception e) {
                android.util.Log.w("RankAct", "同步 " + shortId + " 收获记录异常: " + e.getMessage());
            }
        }
    }

    private long[] getTimeRangeForMode() {
        Calendar cal = Calendar.getInstance();
        long endTime = cal.getTimeInMillis();
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);

        switch (currentMode) {
            case MODE_DAILY:
                return new long[]{cal.getTimeInMillis(), endTime};
            case MODE_WEEKLY:
                cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
                return new long[]{cal.getTimeInMillis(), endTime};
            case MODE_MONTHLY:
                cal.set(Calendar.DAY_OF_MONTH, 1);
                return new long[]{cal.getTimeInMillis(), endTime};
            default:
                return new long[]{0, endTime};
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Activity 不可见时停止自动轮询
        if (autoRefreshHandler != null) {
            autoRefreshHandler.removeCallbacks(autoRefreshRunnable);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Activity 恢复可见时重启自动轮询
        if (autoRefreshHandler != null) {
            autoRefreshHandler.removeCallbacks(autoRefreshRunnable);
            autoRefreshHandler.postDelayed(autoRefreshRunnable, AUTO_REFRESH_MS);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (autoRefreshHandler != null) {
            autoRefreshHandler.removeCallbacks(autoRefreshRunnable);
            autoRefreshHandler = null;
        }
        binding = null;
    }
}
