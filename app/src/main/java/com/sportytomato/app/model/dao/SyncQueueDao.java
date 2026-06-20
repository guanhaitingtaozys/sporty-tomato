package com.sportytomato.app.model.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.sportytomato.app.model.entity.SyncQueue;

import java.util.List;

@Dao
public interface SyncQueueDao {

    @Insert
    void insert(SyncQueue task);

    /** 按用户获取所有待发送/失败的任务 */
    @Query("SELECT * FROM sync_queue WHERE userId = :userId AND (status = 0 OR status = 2) AND nextRetryAt <= :now ORDER BY createdAt ASC")
    List<SyncQueue> getPendingTasks(int userId, long now);

    /** 标记任务已发送 */
    @Query("UPDATE sync_queue SET status = 1 WHERE id = :id")
    void markSent(long id);

    /** 标记失败并递增重试次数（指数退避由调用方计算nextRetryAt） */
    @Update
    void update(SyncQueue task);

    /** 删除指定用户所有已完成任务 */
    @Query("DELETE FROM sync_queue WHERE userId = :userId AND status = 1")
    void deleteCompleted(int userId);

    /** 获取失败计数 */
    @Query("SELECT COUNT(*) FROM sync_queue WHERE userId = :userId AND status = 2")
    int getFailedCount(int userId);
}
