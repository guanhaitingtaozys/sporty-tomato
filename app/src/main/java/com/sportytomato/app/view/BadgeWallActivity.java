package com.sportytomato.app.view;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.sportytomato.app.R;
import com.sportytomato.app.databinding.ActivityBadgeWallBinding;
import com.sportytomato.app.model.entity.User;
import com.sportytomato.app.service.AchievementService;
import com.sportytomato.app.service.RepositoryManager;
import com.sportytomato.app.view.adapter.BadgeAdapter;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * 勋章墙页面：展示所有勋章及其解锁状态
 *
 * 统一文生图提示词（用于 AI 生成勋章图标）：
 * "为手游'运动番茄'设计一枚正方形成就勋章图标，勋章名称：[勋章名]。
 * 图标风格：扁平化矢量风格、可爱卡通、色彩鲜明、无背景。
 * 要求：正方形 1024x1024、高清、纯图标不含文字、适合游戏勋章展示。"
 *
 * 勋章名称列表（共7枚）：初次收获、收获达人、收获大师、辛勤园丁、万步达人、能量大师、好友达人
 */
public class BadgeWallActivity extends AppCompatActivity {

    private ActivityBadgeWallBinding binding;
    private BadgeAdapter adapter;
    private AchievementService achievementService;
    private int currentUserId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityBadgeWallBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        achievementService = ((SportyTomatoApplication) getApplication()).getAchievementService();

        setupToolbar();
        setupRecyclerView();

        // 后台加载用户和勋章
        new Thread(() -> {
            User currentUser = RepositoryManager.getInstance().getUserRepo().getCurrentUser();
            currentUserId = currentUser != null ? currentUser.getUserId() : 1;
            List<AchievementService.BadgeWithStatus> badges =
                    achievementService.getAllBadgesWithStatus(currentUserId);
            long unlockedCount = badges.stream().filter(b -> b.unlocked).count();
            final String summary = "已解锁 " + unlockedCount + "/" + badges.size() + " 枚勋章";
            runOnUiThread(() -> {
                if (isFinishing() || isDestroyed()) return;
                adapter.setBadges(badges);
                binding.tvBadgeSummary.setText(summary);
            });
        }).start();
    }

    private void setupToolbar() {
        binding.toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void setupRecyclerView() {
        adapter = new BadgeAdapter();
        binding.recyclerViewBadges.setLayoutManager(new GridLayoutManager(this, 2));
        binding.recyclerViewBadges.setAdapter(adapter);

        adapter.setOnBadgeClickListener(badge -> {
            // 使用自定义弹窗展示勋章详情
            View dialogView = LayoutInflater.from(this)
                    .inflate(R.layout.dialog_badge_detail, null);

            TextView tvName = dialogView.findViewById(R.id.tv_detail_badge_name);
            ImageView ivIcon = dialogView.findViewById(R.id.iv_detail_badge_icon);
            TextView tvDesc = dialogView.findViewById(R.id.tv_detail_desc);
            TextView tvUnlockInfo = dialogView.findViewById(R.id.tv_detail_unlock_info);

            tvName.setText(badge.template.getBadgeName());
            tvDesc.setText(badge.template.getDescription());

            // 加载实际勋章图片
            int resId = AchievementService.getBadgeIconRes(badge.template.getIconName());
            if (resId != 0) {
                ivIcon.setImageResource(resId);
            }

            if (badge.unlocked) {
                ivIcon.setColorFilter(null);
                ivIcon.setAlpha(1.0f);
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
                tvUnlockInfo.setText("解锁时间: " + sdf.format(new Date(badge.unlockedAt)));
            } else {
                ivIcon.setColorFilter(android.graphics.Color.argb(120, 80, 80, 80));
                ivIcon.setAlpha(0.5f);
                tvUnlockInfo.setText("尚未解锁");
            }

            AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                    .setView(dialogView)
                    .create();

            dialogView.findViewById(R.id.btn_close_detail).setOnClickListener(v -> dialog.dismiss());

            dialog.show();
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}
