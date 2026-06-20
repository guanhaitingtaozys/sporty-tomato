package com.sportytomato.app.view.worker;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.sportytomato.app.model.database.AppDatabase;
import com.sportytomato.app.service.DryPenaltyService;

/**
 * WorkManager 干涸惩罚定期检查 Worker
 * 每2小时触发一次，检查当前登录用户的番茄是否进入干涸状态
 */
public class DryPenaltyWorker extends Worker {

    public DryPenaltyWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        try {
            AppDatabase database = AppDatabase.getInstance(getApplicationContext());
            DryPenaltyService dryPenaltyService = new DryPenaltyService(database, getApplicationContext());

            int userId = getApplicationContext()
                    .getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
                    .getInt("current_user_id", -1);

            if (userId != -1) {
                dryPenaltyService.checkAndApply(userId);
            }

            return Result.success();
        } catch (Exception e) {
            return Result.retry();
        }
    }
}
