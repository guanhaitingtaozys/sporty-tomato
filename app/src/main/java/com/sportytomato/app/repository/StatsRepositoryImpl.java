package com.sportytomato.app.repository;

import com.sportytomato.app.model.database.AppDatabase;

public class StatsRepositoryImpl implements StatsRepository {
    private final AppDatabase database;
    public StatsRepositoryImpl(AppDatabase database) { this.database = database; }
    @Override public int getTodayWateringCount(int userId, String date) { return database.wateringLogDao().getTodayWateringCount(userId, date); }
    @Override public int getTotalHarvestCount(int userId) { return database.harvestLogDao().getTotalHarvestCount(userId); }
    @Override public int getTodayEnergy(int userId, String date) { return database.energyRecordDao().getTodayNetEnergy(userId, date); }
}