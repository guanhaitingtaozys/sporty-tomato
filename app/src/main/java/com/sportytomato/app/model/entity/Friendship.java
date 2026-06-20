package com.sportytomato.app.model.entity;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(tableName = "friendships", indices = {
    @Index(value = {"userId", "friendUserId"}, unique = true)
})
public class Friendship {
    @PrimaryKey(autoGenerate = true) private int id;
    @ColumnInfo private int userId;
    @ColumnInfo private int friendUserId;
    @ColumnInfo private int status;
    @ColumnInfo private String verifyMessage;
    @ColumnInfo private String remarkName;
    @ColumnInfo private long createdAt;
    @ColumnInfo private long updatedAt;
    public Friendship() {}
    public static Friendship createRequest(int fromUserId, int toUserId, String message) { Friendship f = new Friendship(); f.userId = fromUserId; f.friendUserId = toUserId; f.status = 0; f.verifyMessage = message; f.createdAt = System.currentTimeMillis(); f.updatedAt = f.createdAt; return f; }
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }
    public int getFriendUserId() { return friendUserId; }
    public void setFriendUserId(int friendUserId) { this.friendUserId = friendUserId; }
    public int getStatus() { return status; }
    public void setStatus(int status) { this.status = status; }
    public String getVerifyMessage() { return verifyMessage; }
    public void setVerifyMessage(String verifyMessage) { this.verifyMessage = verifyMessage; }
    public String getRemarkName() { return remarkName; }
    public void setRemarkName(String remarkName) { this.remarkName = remarkName; }
    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
    public long getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(long updatedAt) { this.updatedAt = updatedAt; }
}