package com.sportytomato.app.view;

import android.app.Application;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import com.sportytomato.app.model.database.AppDatabase;
import com.sportytomato.app.service.*;
import com.sportytomato.app.util.NotificationHelper;
import com.sportytomato.app.view.worker.DailyResetWorker;
import com.sportytomato.app.view.worker.DryPenaltyWorker;
import java.util.concurrent.TimeUnit;

public class SportyTomatoApplication extends Application {
    private AppDatabase appDatabase;
    private AchievementService achievementService;
    private DryPenaltyService dryPenaltyService;

    @Override
    public void onCreate() {
        super.onCreate();
        androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        appDatabase = AppDatabase.getInstance(this);
        RepositoryManager.getInstance().init(appDatabase, this);
        scheduleDailyReset();
        try { scheduleDryPenaltyCheck(); } catch (Exception e) {}
        achievementService = new AchievementService(appDatabase, this);
        achievementService.initBadgeTemplates();
        dryPenaltyService = new DryPenaltyService(appDatabase, this);
        CloudSyncService.getInstance().init(appDatabase, this);
        NotificationHelper.createChannels(this);
        NotificationHelper.scheduleEnergyReminder(this);
        StepSensorService.getInstance(this).startListening();
        try { HealthConnectService.getInstance(this); } catch (Throwable e) {}
        initEMASClient();
    }

    private void scheduleDailyReset() {
        PeriodicWorkRequest resetWork = new PeriodicWorkRequest.Builder(DailyResetWorker.class, 24, TimeUnit.HOURS)
            .setBackoffCriteria(androidx.work.BackoffPolicy.EXPONENTIAL, 10, TimeUnit.SECONDS).build();
        WorkManager.getInstance(this).enqueueUniquePeriodicWork("daily_reset", ExistingPeriodicWorkPolicy.KEEP, resetWork);
    }

    private void scheduleDryPenaltyCheck() {
        PeriodicWorkRequest dryCheckWork = new PeriodicWorkRequest.Builder(DryPenaltyWorker.class, 2, TimeUnit.HOURS)
            .setBackoffCriteria(androidx.work.BackoffPolicy.EXPONENTIAL, 10, TimeUnit.SECONDS).build();
        WorkManager.getInstance(this).enqueueUniquePeriodicWork("dry_penalty_check", ExistingPeriodicWorkPolicy.KEEP, dryCheckWork);
    }

    private void initEMASClient() {
        String[] cfg = EMASConfig.getCloudCredentials();
        try {
            EMASClient.getInstance().init(cfg[0], cfg[1], cfg[2]);
            android.util.Log.i("SportyTomatoApp", "EMAS Client initialized");
        } catch (Exception e) {
            android.util.Log.e("SportyTomatoApp", "EMASClient init failed: " + e.getMessage());
            RepositoryManager.getInstance().setDataSource(RepositoryManager.DataSource.ROOM);
        }
    }

    @Override public void onTerminate() { super.onTerminate(); RepositoryManager.getInstance().shutdown(); }
    public AppDatabase getAppDatabase() { return appDatabase; }
    public AchievementService getAchievementService() { return achievementService; }
    public DryPenaltyService getDryPenaltyService() { return dryPenaltyService; }
}