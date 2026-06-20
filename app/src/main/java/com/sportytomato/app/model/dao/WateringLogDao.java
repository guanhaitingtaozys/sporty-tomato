package com.sportytomato.app.model.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import com.sportytomato.app.model.entity.WateringLog;

@Dao
public interface WateringLogDao {

    @Insert
    void insertWateringLog(WateringLog log);

    @Query("SELECT * FROM watering_logs WHERE requestId = :requestId AND isDeleted = 0 LIMIT 1")
    WateringLog getByRequestId(String requestId);

    @Query("SELECT COUNT(*) FROM watering_logs WHERE userId = :userId AND date(wateredAt/1000, 'unixepoch') = :date AND isDeleted = 0")
    int getTodayWateringCount(int userId, String date);

    @Query("SELECT COUNT(*) FROM watering_logs WHERE userId = :userId AND wateredAt BETWEEN :startTime AND :endTime AND isDeleted = 0")
    int getPeriodWateringCount(int userId, long startTime, long endTime);

    @Query("SELECT * FROM watering_logs WHERE userId = :userId AND tomatoId = :tomatoId AND isDeleted = 0 ORDER BY wateredAt DESC LIMIT 1")
    WateringLog getLastWatering(int userId, int tomatoId);

    /** 软删除浇水日志 */
    @Query("UPDATE watering_logs SET isDeleted = 1 WHERE logId = :logId")
    void softDelete(long logId);

    /** 注销时删除用户浇水日志 */
    @Query("DELETE FROM watering_logs WHERE userId = :userId")
    void deleteByUserId(int userId);

    /** 获取用户累计浇水总次数 */
    @Query("SELECT COUNT(*) FROM watering_logs WHERE userId = :userId AND isDeleted = 0")
    int getTotalWateringCount(int userId);
}
