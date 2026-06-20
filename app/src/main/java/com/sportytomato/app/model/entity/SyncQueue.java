package com.sportytomato.app.model.entity;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(tableName = "sync_queue", indices = {
    @Index(value = "userId"),
    @Index(value = "status")
})
public class SyncQueue {
    @PrimaryKey(autoGenerate = true) private long id;
    private int userId;
    private String action;
    private String collection;
    private String payload;
    @ColumnInfo(defaultValue = "0") private int status;
    @ColumnInfo(defaultValue = "0") private int retryCount;
    private long nextRetryAt;
    private long createdAt;
    private String lastError;
    public SyncQueue() {}
    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }
    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }
    public String getCollection() { return collection; }
    public void setCollection(String collection) { this.collection = collection; }
    public String getPayload() { return payload; }
    public void setPayload(String payload) { this.payload = payload; }
    public int getStatus() { return status; }
    public void setStatus(int status) { this.status = status; }
    public int getRetryCount() { return retryCount; }
    public void setRetryCount(int retryCount) { this.retryCount = retryCount; }
    public long getNextRetryAt() { return nextRetryAt; }
    public void setNextRetryAt(long nextRetryAt) { this.nextRetryAt = nextRetryAt; }
    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
    public String getLastError() { return lastError; }
    public void setLastError(String lastError) { this.lastError = lastError; }
}