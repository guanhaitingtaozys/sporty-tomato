package com.sportytomato.app.repository;

import androidx.lifecycle.LiveData;
import com.sportytomato.app.model.entity.HarvestLog;
import java.util.List;

public interface HarvestRepository {
    int getTotalHarvestCount(int userId);
    List<HarvestLog> getHarvestHistory(int userId);
    LiveData<Integer> getTotalHarvestCountLive(int userId);
    HarvestLog getByRequestId(String requestId);
    void insertHarvestLog(HarvestLog log);
    int getPeriodHarvestCount(int userId, long startTime, long endTime);
}