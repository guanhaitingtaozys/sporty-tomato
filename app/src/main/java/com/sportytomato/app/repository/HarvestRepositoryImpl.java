package com.sportytomato.app.repository;

import androidx.lifecycle.LiveData;
import com.sportytomato.app.model.database.AppDatabase;
import com.sportytomato.app.model.entity.HarvestLog;
import java.util.List;

public class HarvestRepositoryImpl implements HarvestRepository {
    private final AppDatabase database;
    public HarvestRepositoryImpl(AppDatabase database) { this.database = database; }
    @Override public int getTotalHarvestCount(int userId) { return database.harvestLogDao().getTotalHarvestCount(userId); }
    @Override public List<HarvestLog> getHarvestHistory(int userId) { return database.harvestLogDao().getHarvestHistory(userId); }
    @Override public LiveData<Integer> getTotalHarvestCountLive(int userId) { return database.harvestLogDao().getTotalHarvestCountLive(userId); }
    @Override public HarvestLog getByRequestId(String requestId) { return database.harvestLogDao().getByRequestId(requestId); }
    @Override public void insertHarvestLog(HarvestLog log) { database.harvestLogDao().insertHarvestLog(log); }
    @Override public int getPeriodHarvestCount(int userId, long startTime, long endTime) { return database.harvestLogDao().getPeriodHarvestCount(userId, startTime, endTime); }
}