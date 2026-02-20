package com.blueberryjoy.notes.message;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import com.blueberryjoy.notes.App;

public class Message {
    public static void show(String str) {
        Toast.makeText(App.Companion.getInstance().getContext(), str, Toast.LENGTH_LONG).show();
    }

    public static void showFromThread(String str) {
        Handler handler = new Handler(Looper.getMainLooper());
        handler.post(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(App.Companion.getInstance().getContext(), str, Toast.LENGTH_LONG).show();
            }
        });
    }
}
