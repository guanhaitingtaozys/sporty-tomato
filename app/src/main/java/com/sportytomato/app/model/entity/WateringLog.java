package com.sportytomato.app.model.entity;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(tableName = "watering_logs", indices = {
    @Index(value = {"userId", "tomatoId"}),
    @Index(value = "wateredAt"),
    @Index(value = "requestId", unique = true)
})
public class WateringLog {
    @PrimaryKey(autoGenerate = true) private int logId;
    private int userId;
    private int tomatoId;
    @ColumnInfo private int energyConsumed;
    private int progressBefore;
    private int progressAfter;
    private long wateredAt;
    private String requestId;
    @ColumnInfo(defaultValue = "0") private int isDeleted;
    public WateringLog() {}
    public int getLogId() { return logId; }
    public void setLogId(int logId) { this.logId = logId; }
    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }
    public int getTomatoId() { return tomatoId; }
    public void setTomatoId(int tomatoId) { this.tomatoId = tomatoId; }
    public int getEnergyConsumed() { return energyConsumed; }
    public void setEnergyConsumed(int energyConsumed) { this.energyConsumed = energyConsumed; }
    public int getProgressBefore() { return progressBefore; }
    public void setProgressBefore(int progressBefore) { this.progressBefore = progressBefore; }
    public int getProgressAfter() { return progressAfter; }
    public void setProgressAfter(int progressAfter) { this.progressAfter = progressAfter; }
    public long getWateredAt() { return wateredAt; }
    public void setWateredAt(long wateredAt) { this.wateredAt = wateredAt; }
    public String getRequestId() { return requestId; }
    public void setRequestId(String requestId) { this.requestId = requestId; }
    public int getIsDeleted() { return isDeleted; }
    public void setIsDeleted(int isDeleted) { this.isDeleted = isDeleted; }
}