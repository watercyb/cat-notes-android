package com.blueberryjoy.notes.sqlite;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "notes")
public class Note {
    @PrimaryKey(autoGenerate = true)
    public int id;

    final public String title;
    final public String content;
    final public String mongoId;
    final public int version;

    public Note(String title, String content, String mongoId, int version) {
        this.title = title;
        this.content = content;
        this.mongoId = mongoId;
        this.version = version;
    }

    @NonNull
    public String toString() {
        return "id: " + id + " title: " + title + " mongoId: " + mongoId + " version: " + version + " content: " + content;
    }
}