package com.blueberryjoy.notes.sqlite;

import static com.blueberryjoy.notes.sqlite.AppDatabase.MIGRATION_1_2;
import static com.blueberryjoy.notes.sqlite.AppDatabase.MIGRATION_2_3;
import static com.blueberryjoy.notes.sqlite.AppDatabase.MIGRATION_3_4;

import androidx.annotation.NonNull;
import androidx.room.Room;

import com.blueberryjoy.notes.App;
import com.blueberryjoy.notes.utils.RecyclerViewItem;
import com.blueberryjoy.notes.utils.Message;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class SQLite {
    private static final AppDatabase db = Room
            .databaseBuilder(App.Companion.getInstance().getContext(), AppDatabase.class, "app_db")
            .addMigrations(MIGRATION_1_2)
            .addMigrations(MIGRATION_2_3)
            .addMigrations(MIGRATION_3_4)
            .build();
    private static final ExecutorService executor = Executors.newSingleThreadExecutor();

    public static List<SQLNote> initialize() {
        List<SQLNote> SQLNotes = new ArrayList<>();
        Future<List<SQLNote>> future = executor.submit(() -> Objects.requireNonNull(db.pageDao()).getAllNotes());
        try {
            SQLNotes = future.get();
        } catch (InterruptedException | ExecutionException e) {
            Message.debug("Error at SQLite initialize: " + e);
        }
        return SQLNotes;
    }

    public static void saveMongoId(@NonNull RecyclerViewItem note) {
        executor.submit(() -> Objects.requireNonNull(db.pageDao()).updateMongoId(note.id, note.mongoId));
    }

    public static void saveIdx(@NonNull RecyclerViewItem note) {
        executor.submit(() -> Objects.requireNonNull(db.pageDao()).updateIdx(note.id, note.idx));
    }

    public static void saveRemoval(@NonNull RecyclerViewItem note) {
        executor.submit(() -> Objects.requireNonNull(db.pageDao()).updateRemoval(note.id, note.mongoId, note.removeTime, note.isRemovedFromServer));
    }

    public static void saveNote(@NonNull RecyclerViewItem note) {
        executor.submit(() -> Objects.requireNonNull(db.pageDao()).updateNote(note.id, note.title, note.content, note.mongoId, note.version, note.idx));
    }

    public static void saveUserId(@NonNull RecyclerViewItem note) {
        executor.submit(() -> Objects.requireNonNull(db.pageDao()).updateUserId(note.id, note.userId));
    }

    @NonNull
    public static SQLNote create(@NonNull String title, @NonNull String content, @NonNull String mongoId, int version, int idx, int removeTime, boolean isRemovedFromServer, @NonNull String userId, boolean isEncrypted) {
        SQLNote SQLNote = new SQLNote(title, content, mongoId, version, idx, removeTime, isRemovedFromServer, userId, isEncrypted);
        Future<Long> future = executor.submit(() -> Objects.requireNonNull(db.pageDao()).insert(SQLNote));
        try {
            long id = future.get();
            SQLNote.id = (int) id;
        } catch (InterruptedException | ExecutionException e) {
            Message.debug("Error at SQLite create: " + e);
        }
        return SQLNote;
    }

    public static void delete(@NonNull RecyclerViewItem note) {
        executor.submit(() -> Objects.requireNonNull(db.pageDao()).deleteNote(note.id));
    }
}

