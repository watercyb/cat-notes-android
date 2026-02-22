package com.blueberryjoy.notes.message;


import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.blueberryjoy.notes.App;

public class Message {
    public static void show(String str) {
        Toast.makeText(App.Companion.getInstance().getContext(), str, Toast.LENGTH_LONG).show();
    }

    public static void showFromThread(String str) {
        Handler handler = new Handler(Looper.getMainLooper());
        handler.post(() -> Toast.makeText(App.Companion.getInstance().getContext(), str, Toast.LENGTH_LONG).show());
        logE(str);
    }

    public static void logW(@NonNull String str) {
        Log.w("testLog", str);
    }

    public static void logE(@NonNull String str) {
        Log.e("testLog", str);
    }
}
