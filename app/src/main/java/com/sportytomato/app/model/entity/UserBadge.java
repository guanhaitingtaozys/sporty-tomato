package com.sportytomato.app.model.entity;

import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(tableName = "user_badges", indices = {
    @Index(value = {"userId", "badgeId"}, unique = true),
    @Index(value = "userId")
})
public class UserBadge {
    @PrimaryKey(autoGenerate = true) private int id;
    private int userId;
    private int badgeId;
    private long unlockedAt;
    public UserBadge() {}
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }
    public int getBadgeId() { return badgeId; }
    public void setBadgeId(int badgeId) { this.badgeId = badgeId; }
    public long getUnlockedAt() { return unlockedAt; }
    public void setUnlockedAt(long unlockedAt) { this.unlockedAt = unlockedAt; }
}