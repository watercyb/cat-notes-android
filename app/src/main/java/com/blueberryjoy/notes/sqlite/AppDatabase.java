package com.blueberryjoy.notes.sqlite;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.Database;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

@Database(entities = {SQLNote.class}, version = 3)
public abstract class AppDatabase extends RoomDatabase {
    @Nullable
    public abstract PageDao pageDao();

    static final Migration MIGRATION_1_2 = new Migration(1, 2) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase db) {
            // Add removeTime column with default 0
            db.execSQL("ALTER TABLE notes ADD COLUMN removeTime INTEGER NOT NULL DEFAULT 0");
            // Add removedFromServer column with default 0 (false)
            db.execSQL("ALTER TABLE notes ADD COLUMN isRemovedFromServer INTEGER NOT NULL DEFAULT 0");
        }
    };

    static final Migration MIGRATION_2_3 = new Migration(2, 3) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase db) {
            // Add userId column with default ""
            db.execSQL("ALTER TABLE notes ADD COLUMN userId TEXT NOT NULL DEFAULT ''");
        }
    };
}
