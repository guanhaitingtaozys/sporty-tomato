package com.sportytomato.app.view.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.sportytomato.app.R;
import com.sportytomato.app.service.AchievementService;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * 勋章墙适配器：展示解锁/未解锁勋章卡片
 */
public class BadgeAdapter extends RecyclerView.Adapter<BadgeAdapter.ViewHolder> {

    private List<AchievementService.BadgeWithStatus> badges = new ArrayList<>();
    private OnBadgeClickListener listener;

    public interface OnBadgeClickListener {
        void onBadgeClick(AchievementService.BadgeWithStatus badge);
    }

    public void setBadges(List<AchievementService.BadgeWithStatus> badges) {
        this.badges = badges != null ? badges : new ArrayList<>();
        notifyDataSetChanged();
    }

    public void setOnBadgeClickListener(OnBadgeClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_badge, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        AchievementService.BadgeWithStatus badge = badges.get(position);
        holder.bind(badge);
    }

    @Override
    public int getItemCount() {
        return badges.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {

        private final ImageView ivBadgeIcon;
        private final TextView tvBadgeName;
        private final TextView tvBadgeDesc;
        private final TextView tvUnlockTime;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivBadgeIcon = itemView.findViewById(R.id.iv_badge_icon);
            tvBadgeName = itemView.findViewById(R.id.tv_badge_name);
            tvBadgeDesc = itemView.findViewById(R.id.tv_badge_desc);
            tvUnlockTime = itemView.findViewById(R.id.tv_unlock_time);
        }

        void bind(AchievementService.BadgeWithStatus badge) {
            tvBadgeName.setText(badge.template.getBadgeName());
            tvBadgeDesc.setText(badge.template.getDescription());

            // 加载实际勋章图标（无论是否解锁都显示）
            int resId = com.sportytomato.app.service.AchievementService.getBadgeIconRes(badge.template.getIconName());
            if (resId != 0) {
                ivBadgeIcon.setImageResource(resId);
            }

            if (badge.unlocked) {
                // 已解锁：彩色显示
                ivBadgeIcon.setColorFilter(null);
                ivBadgeIcon.setAlpha(1.0f);
                tvBadgeName.setAlpha(1.0f);
                tvBadgeDesc.setAlpha(1.0f);

                // 解锁时间
                if (badge.unlockedAt > 0) {
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
                    tvUnlockTime.setText("解锁时间: " + sdf.format(new Date(badge.unlockedAt)));
                } else {
                    tvUnlockTime.setText("已解锁");
                }
                tvUnlockTime.setVisibility(View.VISIBLE);
            } else {
                // 未解锁：灰度显示
                ivBadgeIcon.setColorFilter(android.graphics.Color.argb(120, 80, 80, 80));
                ivBadgeIcon.setAlpha(0.5f);
                tvBadgeName.setAlpha(0.5f);
                tvBadgeDesc.setAlpha(0.5f);

                tvUnlockTime.setText("未解锁");
                tvUnlockTime.setVisibility(View.VISIBLE);
            }

            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onBadgeClick(badge);
                }
            });
        }
    }
}
