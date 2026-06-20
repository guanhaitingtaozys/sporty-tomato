package com.sportytomato.app.model.entity;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(tableName = "energy_records", indices = {
    @Index(value = {"userId", "recordDate"}),
    @Index(value = "sourceType")
})
public class EnergyRecord {
    @PrimaryKey(autoGenerate = true) private int recordId;
    private int userId;
    private int energyValue;
    private String recordDate;
    private int sourceType;
    @ColumnInfo private int capped;
    private long createdAt;
    public EnergyRecord() {}
    public int getRecordId() { return recordId; }
    public void setRecordId(int recordId) { this.recordId = recordId; }
    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }
    public int getEnergyValue() { return energyValue; }
    public void setEnergyValue(int energyValue) { this.energyValue = energyValue; }
    public String getRecordDate() { return recordDate; }
    public void setRecordDate(String recordDate) { this.recordDate = recordDate; }
    public int getSourceType() { return sourceType; }
    public void setSourceType(int sourceType) { this.sourceType = sourceType; }
    public int getCapped() { return capped; }
    public void setCapped(int capped) { this.capped = capped; }
    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
}