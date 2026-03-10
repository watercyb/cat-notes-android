package com.blueberryjoy.notes.utils;


import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.blueberryjoy.notes.App;
import com.blueberryjoy.notes.BuildConfig;
import com.blueberryjoy.notes.R;

public class Message {
    public static void show(String str) {
        Handler handler = new Handler(Looper.getMainLooper());
        handler.post(() -> Toast.makeText(App.Companion.getInstance().getContext(), str, Toast.LENGTH_LONG).show());
    }

    public static void debug(String str) {
        if (BuildConfig.DEBUG) {
            Handler handler = new Handler(Looper.getMainLooper());
            handler.post(() -> Toast.makeText(App.Companion.getInstance().getContext(), str, Toast.LENGTH_LONG).show());
        }
    }

    public static void logW(@NonNull String str) {
        if (BuildConfig.DEBUG)
            Log.w("testLog", str);
    }

    public static void tooManyRequests() {
        new Handler(Looper.getMainLooper()).post(() -> Toast.makeText(App.Companion.getInstance().getContext(), App.Companion.getInstance().getContext().getString(R.string.too_many_requests), Toast.LENGTH_LONG).show());
    }

    public static void invalidCredentials() {
        new Handler(Looper.getMainLooper()).post(() -> Toast.makeText(App.Companion.getInstance().getContext(), App.Companion.getInstance().getContext().getString(R.string.invalid_credentials), Toast.LENGTH_LONG).show());
    }

    public static void internalError() {
        new Handler(Looper.getMainLooper()).post(() -> Toast.makeText(App.Companion.getInstance().getContext(), App.Companion.getInstance().getContext().getString(R.string.server_internal_error), Toast.LENGTH_LONG).show());
    }

    public static void unexpectedError() {
        new Handler(Looper.getMainLooper()).post(() -> Toast.makeText(App.Companion.getInstance().getContext(), App.Companion.getInstance().getContext().getString(R.string.unexpected_error), Toast.LENGTH_LONG).show());
    }

    public static void cannotReachServer() {
        new Handler(Looper.getMainLooper()).post(() -> Toast.makeText(App.Companion.getInstance().getContext(), App.Companion.getInstance().getContext().getString(R.string.unable_connect_to_server), Toast.LENGTH_LONG).show());
    }

    public static void allFieldsRequired() {
        new Handler(Looper.getMainLooper()).post(() -> Toast.makeText(App.Companion.getInstance().getContext(), App.Companion.getInstance().getContext().getString(R.string.all_fields_required), Toast.LENGTH_LONG).show());
    }

    public static void userExists() {
        new Handler(Looper.getMainLooper()).post(() -> Toast.makeText(App.Companion.getInstance().getContext(), App.Companion.getInstance().getContext().getString(R.string.user_already_exist), Toast.LENGTH_LONG).show());
    }

    public static void usernameChangeDisable() {
        new Handler(Looper.getMainLooper()).post(() -> Toast.makeText(App.Companion.getInstance().getContext(), App.Companion.getInstance().getContext().getString(R.string.username_change_no_for_this_acount), Toast.LENGTH_LONG).show());
    }

    public static void passwordChangeDisable() {
        new Handler(Looper.getMainLooper()).post(() -> Toast.makeText(App.Companion.getInstance().getContext(), App.Companion.getInstance().getContext().getString(R.string.password_change_no_for_this_account), Toast.LENGTH_LONG).show());
    }

    public static void invalidVerificationCode() {
        new Handler(Looper.getMainLooper()).post(() -> Toast.makeText(App.Companion.getInstance().getContext(), App.Companion.getInstance().getContext().getString(R.string.invalid_verification_code), Toast.LENGTH_LONG).show());
    }

    public static void unableVerifyEmail() {
        new Handler(Looper.getMainLooper()).post(() -> Toast.makeText(App.Companion.getInstance().getContext(), App.Companion.getInstance().getContext().getString(R.string.unable_verify_email), Toast.LENGTH_LONG).show());
    }
}
