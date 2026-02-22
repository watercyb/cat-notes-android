package com.blueberryjoy.notes.notes;

import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.blueberryjoy.notes.App;
import com.blueberryjoy.notes.main.RecyclerViewItem;
import com.blueberryjoy.notes.message.Message;
import com.blueberryjoy.notes.sqlite.Note;
import com.blueberryjoy.notes.sqlite.SQLite;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class Notes {
    @NonNull
    private static List<RecyclerViewItem> list = new ArrayList<>();

    public static void initialize() {
        List<Note> notes = SQLite.initialize();
        list = new ArrayList<>();
        for (Note note : notes) {
            list.add(new RecyclerViewItem(note));
        }
        list.sort(Comparator.comparingInt(a -> a.idx));
        for (int i = 0; i < list.size(); i++) {
            list.get(i).idx = i * 10000;
        }
    }

    @NonNull
    public static List<RecyclerViewItem> getList() {
        return list;
    }

    @Nullable
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
            int idx = (list.isEmpty() ? 0 : list.get(list.size() - 1).idx) + 10000;
            Note note = SQLite.create(title, content, mongoId, version, idx);
            RecyclerViewItem item = new RecyclerViewItem(note);
            list.add(item);
            return item.id;
        } catch (Exception e) {
            Toast.makeText(App.Companion.getInstance(), "error in Notes create: " + e, Toast.LENGTH_LONG).show();
            Log.e("testLog", String.valueOf(e));
            return -1;
        }
    }

    public static void remove(@NonNull RecyclerViewItem note) {
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).id == note.id) {
                list.remove(i);
                break;
            }
        }
    }

    public static void swap(int i, int j) {
        if (i > j) {
            while (i > j) {
                RecyclerViewItem a = list.get(i);
                RecyclerViewItem b = list.get(i - 1);
                int temp = a.idx;
                a.idx = b.idx;
                b.idx = temp;
                list.set(i, b);
                list.set(i - 1, a);
                SQLite.saveIdx(a);
                SQLite.saveIdx(b);
                i--;
            }
        } else {
            while (i < j) {
                RecyclerViewItem a = list.get(i);
                RecyclerViewItem b = list.get(i + 1);
                int temp = a.idx;
                a.idx = b.idx;
                b.idx = temp;
                list.set(i, b);
                list.set(i + 1, a);
                SQLite.saveIdx(a);
                SQLite.saveIdx(b);
                i++;
            }
        }
    }
}
