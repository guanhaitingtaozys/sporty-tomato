package com.sportytomato.app.model.entity;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(tableName = "users", indices = {
    @Index(value = "localId", unique = true),
    @Index(value = "phone", unique = true),
    @Index(value = "shortId", unique = true),
    @Index(value = {"userId", "deleted"})
})
public class User {
    @PrimaryKey(autoGenerate = true) private int userId;
    @ColumnInfo private String openId;
    @ColumnInfo private String localId;
    @ColumnInfo private String nickname;
    @ColumnInfo private String avatarUrl;
    @ColumnInfo private String shortId;
    @ColumnInfo private int userType;
    @ColumnInfo private String unionId;
    @ColumnInfo private String wxNickname;
    @ColumnInfo private String wxAvatar;
    @ColumnInfo private String phone;
    @ColumnInfo private String passwordHash;
    private long createdAt;
    private long updatedAt;
    @ColumnInfo private int deleted;
    public User() {}
    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }
    public String getOpenId() { return openId; }
    public void setOpenId(String openId) { this.openId = openId; }
    public String getLocalId() { return localId; }
    public void setLocalId(String localId) { this.localId = localId; }
    public String getNickname() { return nickname; }
    public void setNickname(String nickname) { this.nickname = nickname; }
    public String getAvatarUrl() { return avatarUrl; }
    public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }
    public String getShortId() { return shortId; }
    public void setShortId(String shortId) { this.shortId = shortId; }
    public int getUserType() { return userType; }
    public void setUserType(int userType) { this.userType = userType; }
    public String getUnionId() { return unionId; }
    public void setUnionId(String unionId) { this.unionId = unionId; }
    public String getWxNickname() { return wxNickname; }
    public void setWxNickname(String wxNickname) { this.wxNickname = wxNickname; }
    public String getWxAvatar() { return wxAvatar; }
    public void setWxAvatar(String wxAvatar) { this.wxAvatar = wxAvatar; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
    public long getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(long updatedAt) { this.updatedAt = updatedAt; }
    public int getDeleted() { return deleted; }
    public void setDeleted(int deleted) { this.deleted = deleted; }
}