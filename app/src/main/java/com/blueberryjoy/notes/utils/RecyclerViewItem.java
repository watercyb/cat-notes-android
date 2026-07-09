package com.blueberryjoy.notes.utils;

import androidx.annotation.NonNull;

import com.blueberryjoy.notes.retrofit.data_class.ApiNote;
import com.blueberryjoy.notes.sqlite.SQLNote;

import java.time.ZoneId;
import java.util.Objects;

public class RecyclerViewItem {
    public final int id;
    public String title;
    public String content;
    public String mongoId;
    public int version;
    public int idx;
    public long removeTime;
    public boolean isRemovedFromServer;
    public String userId;
    public final boolean isEncrypted;

    public RecyclerViewItem(@NonNull SQLNote SQLNote) {
        id = SQLNote.id;
        title = SQLNote.title;
        content = SQLNote.content;
        mongoId = SQLNote.mongoId;
        version = SQLNote.version;
        idx = SQLNote.idx;
        removeTime = SQLNote.removeTime;
        isRemovedFromServer = SQLNote.isRemovedFromServer;
        userId = SQLNote.userId;
        isEncrypted = SQLNote.isEncrypted;
    }

    public void toRemovedNote(long removeTime, boolean isRemovedFromServer) {
        this.removeTime = removeTime;
        this.isRemovedFromServer = isRemovedFromServer;
    }

    public boolean equals(ApiNote note) {
        return Objects.equals(note.getTitle(), title) && Objects.equals(note.getContent(), content) && note.getMongoId().equals(mongoId) && note.getVersion() == version;
    }

    @NonNull
    public String toString() {
        if (removeTime == 0) {
            return "id: " + id + ", mongoId: " + mongoId + ", title: " + title + ", isEncrypted: " + isEncrypted + ", userId: " + userId;
        } else {
            return "id: " + id + ", mongoId: " + mongoId + ", title: " + title + ", isEncrypted: " + isEncrypted + ", removeTime: " + getDate() + ", isRemovedFromServer: " + isRemovedFromServer + ", userId: " + userId;
        }
    }

    public String getDate() {
        return java.time.Instant.ofEpochMilli(removeTime).atZone(ZoneId.systemDefault()).toLocalDateTime().toString();
    }
}
