package com.blueberryjoy.notes.sqlite;

import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.room.Room;

import com.blueberryjoy.notes.App;
import com.blueberryjoy.notes.main.RecyclerViewItem;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class SQLite {
    private static final AppDatabase db = Room.databaseBuilder(App.Companion.getInstance().getContext(), AppDatabase.class, "app_db").build();
    private static final ExecutorService executor = Executors.newSingleThreadExecutor();

    public static List<Note> initialize() {
        List<Note> notes = new ArrayList<>();
        Future<List<Note>> future = executor.submit(() -> db.pageDao().getAllNotes());
        try {
            notes = future.get();
        } catch (InterruptedException | ExecutionException e) {
            Toast.makeText(App.Companion.getInstance(), "error at SQLite initialize: " + e, Toast.LENGTH_LONG).show();
        }
        return notes;
    }

    public static void clearAll() {
        executor.submit(() -> db.pageDao().deleteAllNotes());
    }

    public static void saveTitle(int id, String title) {
        executor.submit(() -> db.pageDao().updateTitle(id, title));
    }

    public static void saveContent(int id, String content) {
        executor.submit(() -> db.pageDao().updateContent(id, content));
    }

    public static void saveMongoId(int id, String mongoId) {
        executor.submit(() -> db.pageDao().updateMongoId(id, mongoId));
    }

    public static void saveVersion(int id, int version) {
        executor.submit(() -> db.pageDao().updateVersion(id, version));
    }

    public static void saveNote(RecyclerViewItem note) {
        executor.submit(() -> db.pageDao().updateNote(note.id, note.title, note.content, note.mongoId, note.version));
    }

    @NonNull
    public static Note create(String title, String content, String mongoId, int version) {
        Note note = new Note(title, content, mongoId, version);
        Future<Long> future = executor.submit(() -> db.pageDao().insert(note));
        try {
            long id = future.get();
            note.id = (int) id;
        } catch (InterruptedException | ExecutionException e) {
            Toast.makeText(App.Companion.getInstance(), "error at SQLite create: " + e, Toast.LENGTH_LONG).show();
        }
        return note;
    }

    public static void delete(int id) {
        executor.submit(() -> db.pageDao().deleteNote(id));
    }

    public static void delete(RecyclerViewItem note) {
        executor.submit(() -> db.pageDao().deleteNote(note.id));
    }
}

