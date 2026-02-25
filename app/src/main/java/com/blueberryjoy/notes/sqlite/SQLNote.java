package com.blueberryjoy.notes.sqlite;

import androidx.annotation.NonNull;
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

    public SQLNote(String title, String content, String mongoId, int version, int idx) {
        this.title = title;
        this.content = content;
        this.mongoId = mongoId;
        this.version = version;
        this.idx = idx;
    }

    @NonNull
    public String toString() {
        return "id: " + id + " title: " + title + " mongoId: " + mongoId + " version: " + version + " content: " + content + " idx: " + idx;
    }
}