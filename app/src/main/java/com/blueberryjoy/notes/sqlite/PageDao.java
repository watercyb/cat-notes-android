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

    @Query("UPDATE notes SET mongoId = :mongoId WHERE id = :id")
    void updateMongoId(int id, String mongoId);

    @Query("UPDATE notes SET idx = :idx WHERE id = :id")
    void updateIdx(int id, int idx);

    @Query("UPDATE notes SET removeTime = :removeTime, mongoId = :mongoId, isRemovedFromServer = :isRemovedFromServer WHERE id = :id")
    void updateRemoval(int id, String mongoId, long removeTime, boolean isRemovedFromServer);

    @Query("UPDATE notes SET userId = :userId WHERE id = :id")
    void updateUserId(int id, String userId);

    @Query("UPDATE notes SET title = :title, content = :content, mongoId = :mongoId, version = :version, idx = :idx WHERE id = :id")
    void updateNote(int id, String title, String content, String mongoId, int version, int idx);
}
