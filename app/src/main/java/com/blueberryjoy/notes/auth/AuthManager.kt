package com.blueberryjoy.notes.auth

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Handler
import android.os.Looper
import android.util.TypedValue
import androidx.core.content.edit
import com.blueberryjoy.notes.App
import com.blueberryjoy.notes.App.Companion.getInstance
import com.blueberryjoy.notes.message.Message
import com.blueberryjoy.notes.utils.AuthType
import com.blueberryjoy.notes.utils.Avatar
import com.blueberryjoy.notes.utils.OnAuthChangeHandler
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.clerk.api.Clerk
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.Executors


object AuthManager {
    private var token: String? = ""

    private var type: AuthType? = AuthType.NULL
    private var username: String? = ""
    private var onAuthChangeHandler: OnAuthChangeHandler? = null
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

        CoroutineScope(Dispatchers.Main).launch {
            if (AuthManager.onAuthChangeHandler != null) AuthManager.onAuthChangeHandler!!.updateAvatar(
                getAvatar()
            )
        }
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
        CoroutineScope(Dispatchers.Main).launch {
            if (onAuthChangeHandler != null) onAuthChangeHandler!!.updateAvatar(getAvatar())
        }
        sharedPref!!.edit {
            putInt("TYPE", 1)
            putString("NAME", username)
        }
    }

    @JvmStatic
    fun setGoogleLogin(): Boolean {
        type = AuthType.GOOGLE
        username = ""
        CoroutineScope(Dispatchers.Main).launch {
            if (onAuthChangeHandler != null) onAuthChangeHandler!!.updateAvatar(getAvatar())
        }
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
        CoroutineScope(Dispatchers.Main).launch {
            if (onAuthChangeHandler != null) onAuthChangeHandler!!.updateAvatar(getAvatar())
        }
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

    @JvmStatic
    val avatar: String
        get() = Clerk.user?.imageUrl ?: ""

    suspend fun getAvatar(): Bitmap? {
        when (type) {
            AuthType.GOOGLE -> {
                val url = Clerk.user?.imageUrl ?: ""
                if (!url.isEmpty()) {
                    val image = loadClerkImage(url)
                    if (image != null) {
                        saveClerkAvatarToFile(image)
                        return image
                    }
                }
                return getClerkAvatarFromFile()
            }

            AuthType.EMAIL -> {
                val username = getUsername()
                if (username != null) {
                    val size = TypedValue.applyDimension(
                        TypedValue.COMPLEX_UNIT_DIP,
                        35f,
                        getInstance().getContext().resources.displayMetrics
                    ).toInt()
                    return Avatar.createLetterAvatar(username, size)
                }
                return null
            }

            else -> {
                return null
            }
        }
    }

    private suspend fun loadClerkImage(url: String): Bitmap? {
        return try {
            withContext(Dispatchers.IO) {
                Glide.with(App.getInstance().getContext()).asBitmap().load(url)
                    .apply(RequestOptions.circleCropTransform()).submit().get()
            }
        } catch (e: Exception) {
            null;
        }
    }

    private fun saveClerkAvatarToFile(bitmap: Bitmap) {
        Executors.newSingleThreadExecutor().execute {
            try {
                val folder = File(getInstance().getContext().filesDir, "avatar")
                if (!folder.exists())
                    folder.mkdirs()

                val file = File(getInstance().getContext().filesDir, "google.png")
                if (!file.exists())
                    file.createNewFile()

                FileOutputStream(file).use { out ->
                    bitmap.compress(
                        Bitmap.CompressFormat.PNG, 100, out
                    )
                }
            } catch (e: Exception) {
                Message.showFromThread("Save avatar: $e")
            }
        }
    }

    private fun getClerkAvatarFromFile(): Bitmap? {
        val file = File(getInstance().getContext().filesDir, "google.png")
        if (file.exists()) {
            val bitmap: Bitmap = BitmapFactory.decodeFile(file.absolutePath)
            return bitmap;
        }
        return null
    }

    @JvmStatic
    val isLogin: Boolean
        get() = Clerk.user != null

    @JvmStatic
    fun getType(): AuthType? {
        return type
    }
}
