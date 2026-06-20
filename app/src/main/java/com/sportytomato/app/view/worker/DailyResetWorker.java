package com.sportytomato.app.view.worker;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.sportytomato.app.model.database.AppDatabase;
import com.sportytomato.app.service.ResetService;

/**
 * WorkManager 每日重置 Worker
 * 在每天零点触发，重置当日步数和能量记录
 */
public class DailyResetWorker extends Worker {

    public DailyResetWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        try {
            AppDatabase database = AppDatabase.getInstance(getApplicationContext());
            ResetService resetService = new ResetService(database, getApplicationContext());
            resetService.performDailyReset();
            return Result.success();
        } catch (Exception e) {
            return Result.retry();
        }
    }
}
