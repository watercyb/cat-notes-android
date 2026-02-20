package com.blueberryjoy.notes.notes;

import android.util.Log;
import android.widget.Toast;

import com.blueberryjoy.notes.App;
import com.blueberryjoy.notes.main.RecyclerViewItem;
import com.blueberryjoy.notes.sqlite.Note;
import com.blueberryjoy.notes.sqlite.SQLite;

import java.util.ArrayList;
import java.util.List;

public class Notes {
    private static List<RecyclerViewItem> list = new ArrayList<>();

    public static void initialize() {
        List<Note> notes = SQLite.initialize();
        list = new ArrayList<>();
        for (Note note : notes) {
            list.add(new RecyclerViewItem(note));
        }
    }

    public static List<RecyclerViewItem> getList() {
        return list;
    }

    public static RecyclerViewItem getNoteById(int id) {
        for (RecyclerViewItem note : list) {
            if (note.id == id) return note;
        }
        return null;
    }

    public static int getNoteByIdx(int id) {
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).id == id) return i;
        }
        return -1;
    }

    public static int create(String title, String content, String mongoId, int version) {
        try {
            Note note = SQLite.create("", "", null, 0);
            RecyclerViewItem item = new RecyclerViewItem(note);
            list.add(item);
            return item.id;
        } catch (Exception e) {
            Toast.makeText(App.Companion.getInstance(), "error in Notes create: " + e, Toast.LENGTH_LONG).show();
            Log.e("testLog", String.valueOf(e));
            return -1;
        }
    }

    public static void remove(RecyclerViewItem note) {
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).id == note.id) {
                list.remove(i);
                break;
            }
        }
    }
}
