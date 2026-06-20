package com.sportytomato.app.model.entity;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(tableName = "step_records", indices = {
    @Index(value = {"userId", "recordDate"}),
    @Index(value = "synced")
})
public class StepRecord {
    @PrimaryKey(autoGenerate = true) private int recordId;
    private int userId;
    private int stepCount;
    private String recordDate;
    private long recordTime;
    @ColumnInfo private int sourceType;
    @ColumnInfo private int synced;
    public StepRecord() {}
    public int getRecordId() { return recordId; }
    public void setRecordId(int recordId) { this.recordId = recordId; }
    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }
    public int getStepCount() { return stepCount; }
    public void setStepCount(int stepCount) { this.stepCount = stepCount; }
    public String getRecordDate() { return recordDate; }
    public void setRecordDate(String recordDate) { this.recordDate = recordDate; }
    public long getRecordTime() { return recordTime; }
    public void setRecordTime(long recordTime) { this.recordTime = recordTime; }
    public int getSourceType() { return sourceType; }
    public void setSourceType(int sourceType) { this.sourceType = sourceType; }
    public int getSynced() { return synced; }
    public void setSynced(int synced) { this.synced = synced; }
}