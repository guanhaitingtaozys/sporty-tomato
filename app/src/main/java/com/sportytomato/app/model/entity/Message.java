package com.sportytomato.app.model.entity;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(tableName = "messages", indices = {
    @Index(value = {"fromUserId", "toUserId"}),
    @Index(value = {"timestamp"})
})
public class Message {
    @PrimaryKey(autoGenerate = true) private int id;
    @ColumnInfo private int fromUserId;
    @ColumnInfo private int toUserId;
    @ColumnInfo private String content;
    @ColumnInfo private int type;
    @ColumnInfo private long timestamp;
    @ColumnInfo private int read;
    public Message() {}
    public static Message createNormal(int from, int to, String content) { Message m = new Message(); m.fromUserId = from; m.toUserId = to; m.content = content; m.type = 0; m.timestamp = System.currentTimeMillis(); m.read = 0; return m; }
    public static Message createSystem(int from, int to, String content, int type) { Message m = new Message(); m.fromUserId = from; m.toUserId = to; m.content = content; m.type = type; m.timestamp = System.currentTimeMillis(); m.read = 0; return m; }
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getFromUserId() { return fromUserId; }
    public void setFromUserId(int fromUserId) { this.fromUserId = fromUserId; }
    public int getToUserId() { return toUserId; }
    public void setToUserId(int toUserId) { this.toUserId = toUserId; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public int getType() { return type; }
    public void setType(int type) { this.type = type; }
    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
    public int getRead() { return read; }
    public void setRead(int read) { this.read = read; }
}