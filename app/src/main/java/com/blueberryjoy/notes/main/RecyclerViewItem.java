package com.blueberryjoy.notes.main;

import com.blueberryjoy.notes.sqlite.Note;

public class RecyclerViewItem {
    public int id;
    public String title;
    public String content;
    public String mongoId;
    public int version;

    public RecyclerViewItem(int id, String title, String content, String mongoId, int version) {
        this.id = id;
        this.title = title;
        this.content = content;
        this.mongoId = mongoId;
        this.version = version;
    }

    public RecyclerViewItem(Note note) {
        id = note.id;
        title = note.title;
        content = note.content;
        mongoId = note.mongoId;
        version = note.version;
    }
}
