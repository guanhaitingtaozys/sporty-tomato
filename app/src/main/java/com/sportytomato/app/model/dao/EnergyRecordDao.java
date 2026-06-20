package com.sportytomato.app.model.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import com.sportytomato.app.model.entity.EnergyRecord;

import java.util.List;

@Dao
public interface EnergyRecordDao {

    @Insert
    void insertEnergyRecord(EnergyRecord record);

    @Query("SELECT COALESCE(SUM(CASE WHEN sourceType IN (0, 2) THEN energyValue ELSE -energyValue END), 0) FROM energy_records WHERE userId = :userId AND recordDate = :date")
    int getTodayNetEnergy(int userId, String date);

    @Query("DELETE FROM energy_records WHERE userId = :userId AND recordDate = :date")
    void deleteTodayRecords(int userId, String date);

    @Query("SELECT * FROM energy_records WHERE userId = :userId AND recordDate = :date ORDER BY createdAt DESC")
    List<EnergyRecord> getTodayEnergyRecords(int userId, String date);

    /** 获取指定日期按来源类型的能量合计 */
    @Query("SELECT COALESCE(SUM(energyValue), 0) FROM energy_records WHERE userId = :userId AND recordDate = :date AND sourceType = :sourceType")
    Integer getEnergyBySourceType(int userId, String date, int sourceType);

    /** 获取指定日期的步数转换毛能量（仅 sourceType=0） */
    @Query("SELECT COALESCE(SUM(energyValue), 0) FROM energy_records WHERE userId = :userId AND recordDate = :date AND sourceType = 0")
    int getTodayStepEnergy(int userId, String date);

    /** 获取用户所有能量记录（用于云端同步） */
    @Query("SELECT * FROM energy_records WHERE userId = :userId ORDER BY createdAt DESC")
    List<EnergyRecord> getAllUserRecords(int userId);

    /** 注销时删除用户所有能量记录 */
    @Query("DELETE FROM energy_records WHERE userId = :userId")
    void deleteByUserId(int userId);

    /** 获取用户累计步数转换总能量（仅 sourceType=0，不限日期） */
    @Query("SELECT COALESCE(SUM(energyValue), 0) FROM energy_records WHERE userId = :userId AND sourceType = 0")
    int getTotalStepEnergy(int userId);
}
