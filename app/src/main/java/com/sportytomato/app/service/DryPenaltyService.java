package com.sportytomato.app.service;

import android.content.Context;
import com.sportytomato.app.model.database.AppDatabase;
import com.sportytomato.app.model.entity.TomatoState;
import com.sportytomato.app.util.NotificationHelper;

public class DryPenaltyService {
    private static final long DRY_THRESHOLD_MS = 24 * 60 * 60 * 1000L;
    private static final long WARNING_THRESHOLD_MS = 2 * 60 * 60 * 1000L;
    private static final int DRY_PENALTY_PERCENT = 5;
    private final AppDatabase database;
    private final Context context;

    public DryPenaltyService(AppDatabase database, Context context) { this.database = database; this.context = context; }

    public DryResult checkAndApply(int userId) {
        TomatoState tomato = database.tomatoDao().getTomatoByUserId(userId);
        if (tomato == null) return new DryResult(false, "番茄不存在", 0, 0, false);
        if (tomato.getProgress() >= 100) return new DryResult(false, "番茄已成熟", tomato.getProgress(), 0, false);
        long lastWateredAt = tomato.getLastWateredAt();
        if (lastWateredAt <= 0) return new DryResult(false, "首次种植", tomato.getProgress(), 0, false);
        long elapsed = System.currentTimeMillis() - lastWateredAt;
        long remaining = DRY_THRESHOLD_MS - elapsed;
        if (elapsed >= DRY_THRESHOLD_MS) {
            int oldProgress = tomato.getProgress();
            int newProgress = Math.max(0, oldProgress - DRY_PENALTY_PERCENT);
            int newStage = TomatoService.calculateStage(newProgress);
            database.tomatoDao().updateProgressOnly(tomato.getTomatoId(), newProgress, newStage);
            return new DryResult(true, "干涸惩罚：进度-" + DRY_PENALTY_PERCENT + "%", oldProgress, newProgress, false);
        } else if (remaining <= WARNING_THRESHOLD_MS && remaining > 0) {
            sendDryWarningNotification(remaining);
            return new DryResult(false, "临近干涸预警", tomato.getProgress(), tomato.getProgress(), true);
        }
        return new DryResult(false, "正常", tomato.getProgress(), tomato.getProgress(), false);
    }

    private void sendDryWarningNotification(long remainingMs) {
        long h = remainingMs / (60 * 60 * 1000);
        long m = (remainingMs % (60 * 60 * 1000)) / (60 * 1000);
        NotificationHelper.sendDryWarningNotification(context, h > 0 ? h + "小时" + m + "分钟" : m + "分钟");
    }

    public static class DryResult {
        public boolean penaltyApplied, isWarning;
        public String message;
        public int oldProgress, newProgress;
        public DryResult(boolean penaltyApplied, String message, int oldProgress, int newProgress, boolean isWarning) {
            this.penaltyApplied = penaltyApplied; this.message = message; this.oldProgress = oldProgress; this.newProgress = newProgress; this.isWarning = isWarning;
        }
    }
}