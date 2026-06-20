package com.sportytomato.app.model.entity;

import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(tableName = "merge_logs", indices = {
    @Index(value = "userId"),
    @Index(value = "mergedAt")
})
public class MergeLog {
    @PrimaryKey(autoGenerate = true) private long id;
    private int userId;
    private String collection;
    private String localSnapshot;
    private String remoteSnapshot;
    private String mergedSnapshot;
    private String strategy;
    private long mergedAt;
    public MergeLog() {}
    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }
    public String getCollection() { return collection; }
    public void setCollection(String collection) { this.collection = collection; }
    public String getLocalSnapshot() { return localSnapshot; }
    public void setLocalSnapshot(String localSnapshot) { this.localSnapshot = localSnapshot; }
    public String getRemoteSnapshot() { return remoteSnapshot; }
    public void setRemoteSnapshot(String remoteSnapshot) { this.remoteSnapshot = remoteSnapshot; }
    public String getMergedSnapshot() { return mergedSnapshot; }
    public void setMergedSnapshot(String mergedSnapshot) { this.mergedSnapshot = mergedSnapshot; }
    public String getStrategy() { return strategy; }
    public void setStrategy(String strategy) { this.strategy = strategy; }
    public long getMergedAt() { return mergedAt; }
    public void setMergedAt(long mergedAt) { this.mergedAt = mergedAt; }
}