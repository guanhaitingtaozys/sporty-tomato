package com.sportytomato.app.model.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import com.sportytomato.app.model.entity.MergeLog;

import java.util.List;

@Dao
public interface MergeLogDao {

    @Insert
    void insert(MergeLog log);

    @Query("SELECT * FROM merge_logs WHERE userId = :userId ORDER BY mergedAt DESC LIMIT :limit")
    List<MergeLog> getRecentLogs(int userId, int limit);

    @Query("DELETE FROM merge_logs WHERE mergedAt < :before")
    void deleteOlderThan(long before);
}
