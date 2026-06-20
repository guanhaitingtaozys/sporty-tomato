package com.sportytomato.app.service;

import com.sportytomato.app.model.database.AppDatabase;
import com.sportytomato.app.repository.HarvestRepository;
import com.sportytomato.app.repository.HarvestRepositoryImpl;
import com.sportytomato.app.repository.StatsRepository;
import com.sportytomato.app.repository.StatsRepositoryImpl;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class StatsService {
    private final StatsRepository statsRepository;
    private final HarvestRepository harvestRepository;
    private final AppDatabase database;

    public StatsService(AppDatabase database) {
        this.database = database;
        this.statsRepository = new StatsRepositoryImpl(database);
        this.harvestRepository = new HarvestRepositoryImpl(database);
    }

    public TodayStats getTodayStats(int userId) {
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        int todaySteps = database.stepRecordDao().getTodayTotalSteps(userId, today);
        return new TodayStats(statsRepository.getTodayWateringCount(userId, today), statsRepository.getTodayEnergy(userId, today), todaySteps);
    }

    public TotalStats getTotalStats(int userId) {
        return new TotalStats(harvestRepository.getTotalHarvestCount(userId), database.wateringLogDao().getTotalWateringCount(userId));
    }

    public static class TodayStats { public final int todayWateringCount, todayEnergy, todaySteps; public TodayStats(int w, int e, int s) { todayWateringCount = w; todayEnergy = e; todaySteps = s; } }
    public static class TotalStats { public final int totalHarvestCount, totalWateringCount; public TotalStats(int h, int w) { totalHarvestCount = h; totalWateringCount = w; } }
}