package com.blueberryjoy.notes.utils;


import android.annotation.SuppressLint;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.blueberryjoy.notes.App;
import com.blueberryjoy.notes.BuildConfig;
import com.blueberryjoy.notes.R;

public class Message {
    public static void show(String str) {
        Handler handler = new Handler(Looper.getMainLooper());
        handler.post(() -> toast(str));
    }

    public static void showWithDelay(String str) {
        new Handler(Looper.getMainLooper()).postDelayed(() -> toast(str), 500);
    }

    @SuppressWarnings("deprecation")
    public static void toast(String str) {
        @SuppressLint("InflateParams")
        View view = LayoutInflater.from(App.Companion.getInstance().getContext()).inflate(R.layout.toast, null);
        TextView text = view.findViewById(R.id.toast_text);
        text.setText(str);
        Toast toast = new Toast(App.Companion.getInstance().getContext());
        toast.setDuration(Toast.LENGTH_LONG);
        toast.setView(view);
        toast.show();
    }

    public static void debug(String str) {
        if (BuildConfig.DEBUG) {
            Handler handler = new Handler(Looper.getMainLooper());
            handler.post(() -> toast(str));
        }
        logW(str);
    }

    public static void logW(@NonNull String str) {
        if (BuildConfig.DEBUG) Log.w("testLog", str);
    }

    public static void tooManyRequests() {
        //new Handler(Looper.getMainLooper()).post(() -> Toast.makeText(App.Companion.getInstance().getContext(), App.Companion.getInstance().getContext().getString(R.string.too_many_requests), Toast.LENGTH_LONG).show());
        new Handler(Looper.getMainLooper()).post(() -> toast(App.Companion.getInstance().getContext().getString(R.string.too_many_requests)));
    }

    public static void invalidCredentials() {
        //new Handler(Looper.getMainLooper()).post(() -> Toast.makeText(App.Companion.getInstance().getContext(), App.Companion.getInstance().getContext().getString(R.string.invalid_credentials), Toast.LENGTH_LONG).show());
        new Handler(Looper.getMainLooper()).post(() -> toast(App.Companion.getInstance().getContext().getString(R.string.invalid_credentials)));
    }

    public static void internalError() {
        //new Handler(Looper.getMainLooper()).post(() -> Toast.makeText(App.Companion.getInstance().getContext(), App.Companion.getInstance().getContext().getString(R.string.server_internal_error), Toast.LENGTH_LONG).show());
        new Handler(Looper.getMainLooper()).post(() -> toast(App.Companion.getInstance().getContext().getString(R.string.server_internal_error)));
    }

    public static void unexpectedError() {
        //new Handler(Looper.getMainLooper()).post(() -> Toast.makeText(App.Companion.getInstance().getContext(), App.Companion.getInstance().getContext().getString(R.string.unexpected_error), Toast.LENGTH_LONG).show());
        new Handler(Looper.getMainLooper()).post(() -> toast(App.Companion.getInstance().getContext().getString(R.string.unexpected_error)));
    }

    public static void cannotReachServer() {
        if (BuildConfig.DEBUG)
            //new Handler(Looper.getMainLooper()).post(() -> Toast.makeText(App.Companion.getInstance().getContext(), App.Companion.getInstance().getContext().getString(R.string.unable_connect_to_server), Toast.LENGTH_LONG).show());
            new Handler(Looper.getMainLooper()).post(() -> toast(App.Companion.getInstance().getContext().getString(R.string.unable_connect_to_server)));
    }

    public static void allFieldsRequired() {
        //new Handler(Looper.getMainLooper()).post(() -> Toast.makeText(App.Companion.getInstance().getContext(), App.Companion.getInstance().getContext().getString(R.string.all_fields_required), Toast.LENGTH_LONG).show());
        new Handler(Looper.getMainLooper()).post(() -> toast(App.Companion.getInstance().getContext().getString(R.string.all_fields_required)));
    }

    public static void userExists() {
        //new Handler(Looper.getMainLooper()).post(() -> Toast.makeText(App.Companion.getInstance().getContext(), App.Companion.getInstance().getContext().getString(R.string.user_already_exist), Toast.LENGTH_LONG).show());
        new Handler(Looper.getMainLooper()).post(() -> toast(App.Companion.getInstance().getContext().getString(R.string.user_already_exist)));
    }

    public static void usernameChangeDisable() {
        //new Handler(Looper.getMainLooper()).post(() -> Toast.makeText(App.Companion.getInstance().getContext(), App.Companion.getInstance().getContext().getString(R.string.username_change_no_for_this_account), Toast.LENGTH_LONG).show());
        new Handler(Looper.getMainLooper()).post(() -> toast(App.Companion.getInstance().getContext().getString(R.string.username_change_no_for_this_account)));
    }

    public static void passwordChangeDisable() {
        //new Handler(Looper.getMainLooper()).post(() -> Toast.makeText(App.Companion.getInstance().getContext(), App.Companion.getInstance().getContext().getString(R.string.password_change_no_for_this_account), Toast.LENGTH_LONG).show());
        new Handler(Looper.getMainLooper()).post(() -> toast(App.Companion.getInstance().getContext().getString(R.string.password_change_no_for_this_account)));
    }

    public static void invalidVerificationCode() {
        //new Handler(Looper.getMainLooper()).post(() -> Toast.makeText(App.Companion.getInstance().getContext(), App.Companion.getInstance().getContext().getString(R.string.invalid_verification_code), Toast.LENGTH_LONG).show());
        new Handler(Looper.getMainLooper()).post(() -> toast(App.Companion.getInstance().getContext().getString(R.string.invalid_verification_code)));
    }

    public static void unableVerifyEmail() {
        //new Handler(Looper.getMainLooper()).post(() -> Toast.makeText(App.Companion.getInstance().getContext(), App.Companion.getInstance().getContext().getString(R.string.unable_verify_email), Toast.LENGTH_LONG).show());
        new Handler(Looper.getMainLooper()).post(() -> toast(App.Companion.getInstance().getContext().getString(R.string.unable_verify_email)));
    }
}