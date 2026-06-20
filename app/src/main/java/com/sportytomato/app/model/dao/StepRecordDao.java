package com.sportytomato.app.model.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import com.sportytomato.app.model.entity.StepRecord;

import java.util.List;

@Dao
public interface StepRecordDao {

    @Insert
    void insertStepRecord(StepRecord record);

    @Query("SELECT COALESCE(SUM(stepCount), 0) FROM step_records WHERE userId = :userId AND recordDate = :date")
    int getTodayTotalSteps(int userId, String date);

    @Query("DELETE FROM step_records WHERE userId = :userId AND recordDate = :date")
    void deleteTodayRecords(int userId, String date);

    @Query("SELECT * FROM step_records WHERE userId = :userId")
    List<StepRecord> getUserStepRecords(int userId);

    /** 注销时删除用户步数记录 */
    @Query("DELETE FROM step_records WHERE userId = :userId")
    void deleteByUserId(int userId);
}
