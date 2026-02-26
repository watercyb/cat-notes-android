package com.blueberryjoy.notes.main;

import androidx.annotation.NonNull;

import com.blueberryjoy.notes.retrofit.data_class.ApiNote;
import com.blueberryjoy.notes.sqlite.SQLNote;

import java.util.Objects;

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

    public RecyclerViewItem(@NonNull SQLNote SQLNote) {
        id = SQLNote.id;
        title = SQLNote.title;
        content = SQLNote.content;
        mongoId = SQLNote.mongoId;
        version = SQLNote.version;
        idx = SQLNote.idx;
    }

    public boolean equals(ApiNote note) {
        return Objects.equals(note.getTitle(), title) && Objects.equals(note.getContent(), content) && note.getMongoId().equals(mongoId) && note.getVersion() == version;
    }

    @NonNull
    public String toString() {
        return "id: " + id + ", mongoId: " + mongoId;
    }
}
