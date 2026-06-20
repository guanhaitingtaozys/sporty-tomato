package com.sportytomato.app.view;

import android.Manifest;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.widget.Toast;
import android.os.Handler;
import android.os.Looper;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;
import com.sportytomato.app.R;
import com.sportytomato.app.databinding.ActivityMainBinding;
import com.sportytomato.app.model.database.AppDatabase;
import com.sportytomato.app.model.entity.Message;
import com.sportytomato.app.model.entity.User;
import com.sportytomato.app.repository.UserRepository;
import com.sportytomato.app.repository.emas.EMASUserRepository;
import com.sportytomato.app.service.*;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private static final int REQUEST_ACTIVITY_RECOGNITION = 1001;
    private static final int REQUEST_POST_NOTIFICATIONS = 1002;
    private static final String PREF_DEBUG = "debug_prefs";
    private static final String KEY_DEBUG_ENABLED = "debug_manual_steps_enabled";
    private static final int DEBUG_CLICK_THRESHOLD = 7;
    private static final long BURST_WINDOW_MS = 2000;
    private ActivityMainBinding binding;
    private ViewPager2 viewPager;
    private int profileClickCount = 0;
    private long profileFirstClickTime = 0;
    private Handler messagePollHandler;
    private static final long POLL_MIN_MS = 30000;
    private static final long POLL_MAX_MS = 300000;
    private long currentPollInterval = POLL_MIN_MS;
    private int consecutiveEmptyPolls = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        requestActivityRecognitionPermission();
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q || ContextCompat.checkSelfPermission(this, Manifest.permission.ACTIVITY_RECOGNITION) == PackageManager.PERMISSION_GRANTED)
            StepSensorService.getInstance(this).startListening();
        new Thread(this::requestHealthConnectPermission).start();
        requestNotificationPermission();
        AppDatabase database = ((SportyTomatoApplication) getApplication()).getAppDatabase();
        ResetService resetService = new ResetService(database, this);
        if (resetService.needsReset()) resetService.performDailyReset();
        viewPager = binding.viewPager;
        viewPager.setAdapter(new MainPagerAdapter(this));
        viewPager.setOffscreenPageLimit(3);
        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override public void onPageSelected(int position) { int menuId = getMenuIdForPosition(position); if (menuId != 0) binding.bottomNavigation.setSelectedItemId(menuId); if (position == 3) handleProfileTabClick(); }
        });
        binding.bottomNavigation.setOnItemSelectedListener(item -> {
            int position = getPositionForMenuId(item.getItemId()); if (position >= 0) viewPager.setCurrentItem(position, true);
            if (item.getItemId() == R.id.nav_profile) handleProfileTabClick(); return true;
        });
        binding.bottomNavigation.setSelectedItemId(R.id.nav_home);
        messagePollHandler = new Handler(Looper.getMainLooper());
        pollUnreadMessages();
    }

    private int getPositionForMenuId(int menuId) { if (menuId == R.id.nav_home) return 0; if (menuId == R.id.nav_stats) return 1; if (menuId == R.id.nav_friends) return 2; if (menuId == R.id.nav_profile) return 3; return -1; }
    private int getMenuIdForPosition(int position) { switch (position) { case 0: return R.id.nav_home; case 1: return R.id.nav_stats; case 2: return R.id.nav_friends; case 3: return R.id.nav_profile; default: return 0; } }

    private static class MainPagerAdapter extends FragmentStateAdapter {
        public MainPagerAdapter(@NonNull AppCompatActivity activity) { super(activity); }
        @NonNull @Override public Fragment createFragment(int position) { switch (position) { case 0: return HomeFragment.newInstance(); case 1: return StatsFragment.newInstance(); case 2: return FriendsFragment.newInstance(); case 3: return ProfileFragment.newInstance(); default: return HomeFragment.newInstance(); } }
        @Override public int getItemCount() { return 4; }
    }

    private void handleProfileTabClick() {
        SharedPreferences debugPrefs = getSharedPreferences(PREF_DEBUG, Context.MODE_PRIVATE);
        if (debugPrefs.getBoolean(KEY_DEBUG_ENABLED, false)) return;
        long now = System.currentTimeMillis();
        if (profileFirstClickTime == 0 || (now - profileFirstClickTime) > BURST_WINDOW_MS) { profileClickCount = 0; profileFirstClickTime = now; }
        profileClickCount++;
        if (profileClickCount >= DEBUG_CLICK_THRESHOLD) { debugPrefs.edit().putBoolean(KEY_DEBUG_ENABLED, true).apply(); Toast.makeText(this, "开发者调试模式已激活", Toast.LENGTH_SHORT).show(); profileClickCount = 0; profileFirstClickTime = 0; }
    }

    private void requestActivityRecognitionPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && ContextCompat.checkSelfPermission(this, Manifest.permission.ACTIVITY_RECOGNITION) != PackageManager.PERMISSION_GRANTED)
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACTIVITY_RECOGNITION}, REQUEST_ACTIVITY_RECOGNITION);
    }

    private void requestHealthConnectPermission() { try { HealthConnectService hc = HealthConnectService.getInstance(this); if (hc.isAvailable()) { /* check perm */ } } catch (Exception e) {} }

    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED)
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.POST_NOTIFICATIONS}, REQUEST_POST_NOTIFICATIONS);
    }

    @Override public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_ACTIVITY_RECOGNITION && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)
            StepSensorService.getInstance(this).startListening();
    }

    @Override protected void onDestroy() { super.onDestroy(); if (messagePollHandler != null) { messagePollHandler.removeCallbacksAndMessages(null); messagePollHandler = null; } binding = null; }

    public void triggerMessagePoll() { if (messagePollHandler != null) { currentPollInterval = POLL_MIN_MS; consecutiveEmptyPolls = 0; messagePollHandler.removeCallbacksAndMessages(null); messagePollHandler.post(this::pollUnreadMessages); } }

    private void pollUnreadMessages() {
        if (!EMASClient.getInstance().isInitialized() || !(RepositoryManager.getInstance().getUserRepo().getCurrentUser() != null)) { scheduleNextPoll(); return; }
        final UserRepository userRepo = RepositoryManager.getInstance().getUserRepo();
        final AppDatabase db = ((SportyTomatoApplication) getApplication()).getAppDatabase();
        new Thread(() -> { User currentUser = userRepo.getCurrentUser(); if (currentUser == null || currentUser.getShortId() == null) { runOnUiThread(this::scheduleNextPoll); return; }
            boolean hasNewMessages = false;
            try { JSONObject query = new JSONObject(); query.put("toShortId", currentUser.getShortId()); query.put("read", false);
                String params = EMASClient.buildDbParamsWithOptions("find", "messages", null, query.toString(), null);
                String result = EMASClient.getInstance().callFunction("sporty-api", params);
                if (result != null) { JSONObject json = new JSONObject(result); if (json.optBoolean("success")) { JSONArray arr = json.optJSONArray("result"); if (arr != null && arr.length() > 0) { /* sync messages */ } } }
            } catch (Exception e) {}
            final int unreadCount = db.messageDao().getUnreadMessages(currentUser.getUserId()).size();
            runOnUiThread(() -> { updateFriendsBadge(unreadCount); scheduleNextPoll(); });
        }).start();
    }

    private void scheduleNextPoll() { if (messagePollHandler != null) messagePollHandler.postDelayed(this::pollUnreadMessages, currentPollInterval); }

    public void updateFriendsBadge(int count) {
        if (binding == null) return;
        if (count > 0) { com.google.android.material.badge.BadgeDrawable badge = binding.bottomNavigation.getOrCreateBadge(R.id.nav_friends); badge.setVisible(true); badge.setNumber(count); badge.setBackgroundColor(getResources().getColor(R.color.error)); badge.setBadgeTextColor(getResources().getColor(R.color.background)); }
        else binding.bottomNavigation.removeBadge(R.id.nav_friends);
    }
}