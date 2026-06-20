package com.sportytomato.app.model.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import com.sportytomato.app.model.entity.TomatoState;

@Dao
public interface TomatoDao {

    @Insert
    void insertTomato(TomatoState tomato);

    @Query("SELECT * FROM tomatoes WHERE userId = :userId AND isDeleted = 0 LIMIT 1")
    TomatoState getTomatoByUserId(int userId);

    @Query("SELECT * FROM tomatoes WHERE userId = :userId AND isDeleted = 0 LIMIT 1")
    LiveData<TomatoState> getTomatoByUserIdLive(int userId);

    @Query("UPDATE tomatoes SET progress = :progress, stage = :stage, lastWateredAt = :lastWateredAt WHERE tomatoId = :tomatoId AND isDeleted = 0")
    void updateProgress(int tomatoId, int progress, int stage, long lastWateredAt);

    @Query("UPDATE tomatoes SET progress = 0, stage = 0, harvestedCount = harvestedCount + 1 WHERE tomatoId = :tomatoId AND isDeleted = 0")
    void resetAndIncrementHarvest(int tomatoId);

    @Query("UPDATE tomatoes SET progress = :progress, stage = :stage WHERE tomatoId = :tomatoId AND isDeleted = 0")
    void updateProgressOnly(int tomatoId, int progress, int stage);

    /** 软删除番茄（标记isDeleted=1） */
    @Query("UPDATE tomatoes SET isDeleted = 1 WHERE tomatoId = :tomatoId")
    void softDeleteTomato(int tomatoId);

    /** 注销时删除用户番茄数据 */
    @Query("DELETE FROM tomatoes WHERE userId = :userId")
    void deleteByUserId(int userId);
}
