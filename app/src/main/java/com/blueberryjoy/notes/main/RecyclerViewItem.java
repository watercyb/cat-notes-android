package com.blueberryjoy.notes.main;

import androidx.annotation.NonNull;

import com.blueberryjoy.notes.sqlite.Note;

public class RecyclerViewItem {
    public int id;
    public String title;
    public String content;
    public String mongoId;
    public int version;
    public int idx;

    public RecyclerViewItem(int id, String title, String content, String mongoId, int version, int idx) {
        this.id = id;
        this.title = title;
        this.content = content;
        this.mongoId = mongoId;
        this.version = version;
        this.idx = idx;
    }

    public RecyclerViewItem(@NonNull Note note) {
        id = note.id;
        title = note.title;
        content = note.content;
        mongoId = note.mongoId;
        version = note.version;
        idx = note.idx;
    }
}
