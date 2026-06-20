package com.sportytomato.app.model.entity;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(tableName = "tomatoes", indices = {
    @Index(value = "userId"),
    @Index(value = "isDeleted")
})
public class TomatoState {
    @PrimaryKey(autoGenerate = true) private int tomatoId;
    private int userId;
    @ColumnInfo private int progress;
    @ColumnInfo private int stage;
    private long lastWateredAt;
    private long createdAt;
    @ColumnInfo private int harvestedCount;
    @ColumnInfo(defaultValue = "0") private int isDeleted;
    @ColumnInfo(defaultValue = "0") private int variety;
    @ColumnInfo(defaultValue = "0") private long varietyUnlockedAt;
    public TomatoState() {}
    // getters and setters
    public int getTomatoId() { return tomatoId; }
    public void setTomatoId(int tomatoId) { this.tomatoId = tomatoId; }
    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }
    public int getProgress() { return progress; }
    public void setProgress(int progress) { this.progress = progress; }
    public int getStage() { return stage; }
    public void setStage(int stage) { this.stage = stage; }
    public long getLastWateredAt() { return lastWateredAt; }
    public void setLastWateredAt(long lastWateredAt) { this.lastWateredAt = lastWateredAt; }
    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
    public int getHarvestedCount() { return harvestedCount; }
    public void setHarvestedCount(int harvestedCount) { this.harvestedCount = harvestedCount; }
    public int getIsDeleted() { return isDeleted; }
    public void setIsDeleted(int isDeleted) { this.isDeleted = isDeleted; }
    public int getVariety() { return variety; }
    public void setVariety(int variety) { this.variety = variety; }
    public long getVarietyUnlockedAt() { return varietyUnlockedAt; }
    public void setVarietyUnlockedAt(long varietyUnlockedAt) { this.varietyUnlockedAt = varietyUnlockedAt; }
}