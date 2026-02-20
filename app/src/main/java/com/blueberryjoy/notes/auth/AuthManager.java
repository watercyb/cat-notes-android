package com.blueberryjoy.notes.auth;

import com.blueberryjoy.notes.auth.utils.AuthType;
import com.blueberryjoy.notes.auth.utils.OnAuthChangeHandler;

public class AuthManager {
    private static String token = "";
    private static AuthType type = AuthType.NULL;
    private static String username = "";
    private static OnAuthChangeHandler onAuthChangeHandler;

    public static void setToken(String token) {
        AuthManager.token = token;
    }

    public static void setClerkToken(String token) {
        AuthManager.token = "Bearer " + token;
    }

    public static String getToken() {
        return token;
    }

    public static String getUser() {
        return username;
    }

    public static void setEmailLogin(String username) {
        AuthManager.type = AuthType.EMAIL;
        AuthManager.username = username;
        if (onAuthChangeHandler != null) onAuthChangeHandler.updateAvatar();
    }

    public static void setUsername(String username) {
        AuthManager.username = username;
    }

    public static String getUsername() {
        if (type == AuthType.NULL) return null;
        return username;
    }

    public static void setType(AuthType type) {
        AuthManager.type = type;
    }

    public static AuthType getType() {
        return type;
    }

    public static void setOnAuthChangeHandler(OnAuthChangeHandler onAuthChangeHandler) {
        AuthManager.onAuthChangeHandler = onAuthChangeHandler;
    }
}
