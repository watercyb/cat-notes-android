package com.blueberryjoy.notes.sqlite;

import androidx.annotation.Nullable;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

@Dao
public interface PageDao {
    @Insert
    long insert(SQLNote SQLNote);

    @Nullable
    @Query("SELECT * FROM notes")
    List<SQLNote> getAllNotes();

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

    @Query("UPDATE notes SET idx = :idx WHERE id = :id")
    void updateIdx(int id, int idx);

    @Query("UPDATE notes SET isRemovedFromServer = :isRemovedFromServer WHERE id = :id")
    void updateIsRemovedFromServer(int id, boolean isRemovedFromServer);

    @Query("UPDATE notes SET removeTime = :removeTime, mongoId = :mongoId, isRemovedFromServer = :isRemovedFromServer WHERE id = :id")
    void updateRemoval(int id, String mongoId, long removeTime, boolean isRemovedFromServer);

    @Query("UPDATE notes SET title = :title, content = :content, mongoId = :mongoId, version = :version, idx = :idx WHERE id = :id")
    void updateNote(int id, String title, String content, String mongoId, int version, int idx);


    @Query("DELETE FROM notes")
    void deleteAllNotes();
}
