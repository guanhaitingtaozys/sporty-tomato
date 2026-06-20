package com.sportytomato.app.model.entity;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(tableName = "harvest_logs", indices = {
    @Index(value = {"userId", "tomatoId"}),
    @Index(value = "harvestedAt"),
    @Index(value = "requestId", unique = true)
})
public class HarvestLog {
    @PrimaryKey(autoGenerate = true) private int logId;
    private int userId;
    private int tomatoId;
    private long harvestedAt;
    private String requestId;
    public HarvestLog() {}
    public int getLogId() { return logId; }
    public void setLogId(int logId) { this.logId = logId; }
    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }
    public int getTomatoId() { return tomatoId; }
    public void setTomatoId(int tomatoId) { this.tomatoId = tomatoId; }
    public long getHarvestedAt() { return harvestedAt; }
    public void setHarvestedAt(long harvestedAt) { this.harvestedAt = harvestedAt; }
    public String getRequestId() { return requestId; }
    public void setRequestId(String requestId) { this.requestId = requestId; }
}