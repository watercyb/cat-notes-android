package com.blueberryjoy.notes.sqlite;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "notes")
public class SQLNote {
    @PrimaryKey(autoGenerate = true)
    public int id;

    final public String title;
    final public String content;
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

    public SQLNote(String title, String content, String mongoId, int version, int idx, long removeTime, boolean isRemovedFromServer, @NonNull String userId) {
        this.title = title;
        this.content = content;
        this.mongoId = mongoId;
        this.version = version;
        this.idx = idx;
        this.removeTime = removeTime;
        this.isRemovedFromServer = isRemovedFromServer;
        this.userId = userId;
    }

    @NonNull
    public String toString() {
        return "id: " + id + " title: " + title + " mongoId: " + mongoId + " version: " + version + " content: " + content + " idx: " + idx + " removeTime: " + removeTime + " isRemovedFromServer: " + isRemovedFromServer + " userId: " + userId;
    }
}