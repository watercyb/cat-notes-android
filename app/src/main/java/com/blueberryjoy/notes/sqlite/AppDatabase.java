package com.blueberryjoy.notes.sqlite;

import androidx.annotation.Nullable;
import androidx.room.Database;
import androidx.room.RoomDatabase;

@Database(entities = {SQLNote.class}, version = 1)
public abstract class AppDatabase extends RoomDatabase {
    @Nullable
    public abstract PageDao pageDao();
}