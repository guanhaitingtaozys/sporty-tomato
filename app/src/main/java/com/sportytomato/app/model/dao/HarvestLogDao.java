package com.sportytomato.app.model.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import com.sportytomato.app.model.entity.HarvestLog;

import java.util.List;

@Dao
public interface HarvestLogDao {

    @Insert
    void insertHarvestLog(HarvestLog log);

    @Query("SELECT * FROM harvest_logs WHERE requestId = :requestId LIMIT 1")
    HarvestLog getByRequestId(String requestId);

    @Query("SELECT COUNT(*) FROM harvest_logs WHERE userId = :userId")
    int getTotalHarvestCount(int userId);

    @Query("SELECT COUNT(*) FROM harvest_logs WHERE userId = :userId")
    LiveData<Integer> getTotalHarvestCountLive(int userId);

    @Query("SELECT COUNT(*) FROM harvest_logs WHERE userId = :userId AND harvestedAt BETWEEN :startTime AND :endTime")
    int getPeriodHarvestCount(int userId, long startTime, long endTime);

    @Query("SELECT * FROM harvest_logs WHERE userId = :userId ORDER BY harvestedAt DESC")
    List<HarvestLog> getHarvestHistory(int userId);

    /** 注销时删除用户收获日志 */
    @Query("DELETE FROM harvest_logs WHERE userId = :userId")
    void deleteByUserId(int userId);

    /** 获取用户对指定番茄的最后一次收获记录（用于幂等检查） */
    @Query("SELECT * FROM harvest_logs WHERE userId = :userId AND tomatoId = :tomatoId ORDER BY harvestedAt DESC LIMIT 1")
    HarvestLog getLastHarvest(int userId, int tomatoId);
}
