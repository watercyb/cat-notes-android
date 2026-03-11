package com.blueberryjoy.notes.sqlite;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.Database;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

@Database(entities = {SQLNote.class}, version = 4)
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

    static final Migration MIGRATION_3_4 = new Migration(3, 4) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase db) {
            // Add isEncrypted column with default 0 (false)
            db.execSQL("ALTER TABLE notes ADD COLUMN isEncrypted INTEGER NOT NULL DEFAULT 0");

            // 1️⃣ Create new table with non-nullable fields and default values
            db.execSQL("CREATE TABLE notes_new (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                    "title TEXT NOT NULL DEFAULT '', " +
                    "content TEXT NOT NULL DEFAULT '', " +
                    "mongoId TEXT NOT NULL DEFAULT '', " +
                    "version INTEGER NOT NULL, " +
                    "idx INTEGER NOT NULL, " +
                    "removeTime INTEGER NOT NULL DEFAULT 0, " +
                    "isRemovedFromServer INTEGER NOT NULL DEFAULT 0, " +
                    "userId TEXT NOT NULL DEFAULT '', " +
                    "isEncrypted INTEGER NOT NULL DEFAULT 0" +
                    ")");

            // 2️⃣ Copy existing data
            db.execSQL("INSERT INTO notes_new (" +
                    "id, title, content, mongoId, version, idx, removeTime, isRemovedFromServer, userId, isEncrypted) " +
                    "SELECT " +
                    "id, " +
                    "COALESCE(title, ''), " +
                    "COALESCE(content, ''), " +
                    "COALESCE(mongoId, ''), " +
                    "version, " +
                    "idx, " +
                    "COALESCE(removeTime, 0), " +
                    "COALESCE(isRemovedFromServer, 0), " +
                    "COALESCE(userId, ''), " +
                    "COALESCE(isEncrypted, 0) " +
                    "FROM notes");

            // 3️⃣ Drop old table
            db.execSQL("DROP TABLE notes");

            // 4️⃣ Rename new table
            db.execSQL("ALTER TABLE notes_new RENAME TO notes");
        }
    };
}
