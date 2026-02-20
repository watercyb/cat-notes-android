package com.blueberryjoy.notes.sqlite;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import com.blueberryjoy.notes.main.RecyclerViewItem;

import java.util.List;

@Dao
public interface PageDao {
    @Insert
    long insert(Note note);

    @Query("SELECT * FROM notes")
    List<Note> getAllNotes();

    @Query("DELETE FROM notes WHERE id = :id")
    void deleteNote(int id);

    @Query("UPDATE notes SET title = :title WHERE id = :id")
    void updateTitle(int id, String title);

    @Query("UPDATE notes SET content = :content WHERE id = :id")
    void updateContent(int id, String content);

    @Query("UPDATE notes SET mongoId = :mongoId WHERE id = :id")
    void updateMongoId(int id, String mongoId);

    @Query("UPDATE notes SET version = :version WHERE id = :id")
    void updateVersion(int id, int version);

    @Query("UPDATE notes SET title = :title, content = :content, mongoId = :mongoId, version = :version WHERE id = :id")
    void updateNote(int id, String title, String content, String mongoId, int version);

    @Query("DELETE FROM notes")
    void deleteAllNotes();
}
