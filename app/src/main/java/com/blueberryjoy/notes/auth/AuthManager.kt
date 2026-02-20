package com.blueberryjoy.notes.auth

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.blueberryjoy.notes.App.Companion.getInstance
import com.blueberryjoy.notes.auth.utils.AuthType
import com.blueberryjoy.notes.auth.utils.OnAuthChangeHandler
import com.blueberryjoy.notes.clerk.ClerkAuth.isLogin
import androidx.core.content.edit
import androidx.lifecycle.ViewModelProvider
import com.blueberryjoy.notes.clerk.ClerkAuth
import com.blueberryjoy.notes.clerk.OAuthViewModel

object AuthManager {
    private var token: String? = ""

    @JvmField
    var type: AuthType? = AuthType.NULL
    private var username: String? = ""
    var onAuthChangeHandler: OnAuthChangeHandler? = null
    private var sharedPref: SharedPreferences? = null

    @JvmStatic
    fun initialize(onAuthChangeHandler: OnAuthChangeHandler?) {
        AuthManager.onAuthChangeHandler = onAuthChangeHandler
        sharedPref =
            getInstance().getContext().getSharedPreferences("APP_DATA", Context.MODE_PRIVATE)
        token = sharedPref!!.getString("TOKEN", "")
        val typeCode = sharedPref!!.getInt("TYPE", 0)
        type = when (typeCode) {
            1 -> {
                AuthType.EMAIL
            }

            2 -> {
                AuthType.GOOGLE
            }

            else -> {
                AuthType.NULL
            }
        }
        username = sharedPref!!.getString("NAME", "")
    }

    @JvmStatic
    fun setToken(token: String?) {
        AuthManager.token = token
        sharedPref!!.edit { putString("TOKEN", token) }
    }

    fun setClerkToken(token: String?) {
        AuthManager.token = "Bearer $token"
    }

    @JvmStatic
    fun getToken(): String? {
        return token
    }

    @JvmStatic
    fun setEmailLogin(username: String?) {
        type = AuthType.EMAIL
        AuthManager.username = username
        if (onAuthChangeHandler != null) onAuthChangeHandler!!.updateAvatar()
        sharedPref!!.edit {
            putInt("TYPE", 1)
            putString("NAME", username)
        }
    }

    @JvmStatic
    fun setGoogleLogin(): Boolean {
        if (!isLogin()) return false
        type = AuthType.GOOGLE
        username = ""
        if (onAuthChangeHandler != null) onAuthChangeHandler!!.updateAvatar()
        sharedPref!!.edit {
            putInt("TYPE", 2)
            putString("NAME", "")
        }
        return true
    }

    @JvmStatic
    fun setLogout() {
        type = AuthType.NULL
        username = ""
        token = ""
        if (onAuthChangeHandler != null) onAuthChangeHandler!!.updateAvatar()
        sharedPref!!.edit {
            putInt("TYPE", 0)
            putString("NAME", "")
            putString("TOKEN", "")
        }
    }

    fun setUsername(username: String?) {
        AuthManager.username = username
    }

    @JvmStatic
    fun getUsername(): String? {
        if (type == AuthType.NULL) return null
        return username
    }

    fun updateAvatar() {
        if (onAuthChangeHandler != null) onAuthChangeHandler!!.updateAvatar()
    }

    @JvmStatic
    val avatar: String
        get() = ClerkAuth.getAvatar()

    @JvmStatic
    val isLogin: Boolean
        get() = ClerkAuth.isLogin()
}
