package com.blueberryjoy.notes.sqlite;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "notes")
public class SQLNote {
    @PrimaryKey(autoGenerate = true)
    public int id;

    @NonNull
    @ColumnInfo(defaultValue = "")
    final public String title;
    @NonNull
    @ColumnInfo(defaultValue = "")
    final public String content;
    @NonNull
    @ColumnInfo(defaultValue = "")
    final public String mongoId;
    final public int version;
    final public int idx;

    // New fields #2
    @ColumnInfo(defaultValue = "0")
    public long removeTime;
    @ColumnInfo(defaultValue = "0")
    public boolean isRemovedFromServer;

    // New fields #3
    @NonNull
    @ColumnInfo(defaultValue = "")
    final public String userId;

    // New fields #4
    @ColumnInfo(defaultValue = "0")
    final public boolean isEncrypted;

    public SQLNote(@NonNull String title, @NonNull String content, @NonNull String mongoId, int version, int idx, long removeTime, boolean isRemovedFromServer, @NonNull String userId, boolean isEncrypted) {
        this.title = title;
        this.content = content;
        this.mongoId = mongoId;
        this.version = version;
        this.idx = idx;
        this.removeTime = removeTime;
        this.isRemovedFromServer = isRemovedFromServer;
        this.userId = userId;
        this.isEncrypted = isEncrypted;
    }

    @NonNull
    public String toString() {
        return "id: " + id + " title: " + title + " mongoId: " + mongoId + " isEncrypted: " + isEncrypted + " version: " + version + " removeTime: " + removeTime + " isRemovedFromServer: " + isRemovedFromServer + " userId: " + userId + " content: " + content + " idx: " + idx;
    }
}