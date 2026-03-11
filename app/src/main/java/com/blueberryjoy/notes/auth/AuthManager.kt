package com.blueberryjoy.notes.auth

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.TypedValue
import androidx.core.content.edit
import com.blueberryjoy.notes.App.Companion.getInstance
import com.blueberryjoy.notes.R
import com.blueberryjoy.notes.clerk.ClerkAuth
import com.blueberryjoy.notes.notes.Notes
import com.blueberryjoy.notes.retrofit.ErrorHandler
import com.blueberryjoy.notes.retrofit.RetrofitInstance
import com.blueberryjoy.notes.retrofit.data_class.ApiReq
import com.blueberryjoy.notes.utils.Avatar
import com.blueberryjoy.notes.utils.Message
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.clerk.api.Clerk
import com.clerk.api.Clerk.user
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.Executors


object AuthManager {
    private var token: String? = ""
    private var refreshToken: String? = ""

    private var type: AuthType? = AuthType.NULL
    private var email: String? = ""
    private var username: String? = ""
    private var isVerified: Boolean = false
    private var isShowAllNotes: Boolean = true
    private var onAuthChangeHandler: OnAuthChangeHandler? = null
    private var sharedPref: SharedPreferences? = null

    @JvmStatic
    fun initialize(onAuthChangeHandler: OnAuthChangeHandler?) {
        AuthManager.onAuthChangeHandler = onAuthChangeHandler
        sharedPref =
            getInstance().getContext().getSharedPreferences("APP_DATA", Context.MODE_PRIVATE)
        token = sharedPref!!.getString("TOKEN", "")
        refreshToken = sharedPref!!.getString("REFRESH_TOKEN", "")
        val typeCode = sharedPref!!.getInt("TYPE", 0)
        type = when (typeCode) {
            1 -> {
                //CheckAuth when it is email login immediately.
                CoroutineScope(Dispatchers.Main).launch {
                    checkAuth()
                }
                AuthType.EMAIL
            }

            2 -> {
                //CheckAuth will be call when clerk login automatically.
                AuthType.GOOGLE
            }

            else -> {
                AuthType.NULL
            }
        }

        email = sharedPref!!.getString("EMAIL", "")
        username = sharedPref!!.getString("NAME", "")
        isVerified = sharedPref!!.getBoolean("VERIFY", false)
        isShowAllNotes = sharedPref!!.getBoolean("ALL_NOTES", true)

        //Message.logW("Type: " + getType().toString() + " name: " + getUsername())

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
    fun getToken(): String {
        return token ?: ""
    }

    @JvmStatic
    fun setIsShowAllNotes(isShowAllNotes: Boolean) {
        AuthManager.isShowAllNotes = isShowAllNotes
        sharedPref!!.edit { putBoolean("ALL_NOTES", isShowAllNotes) }
    }

    @JvmStatic
    fun getIsShowAllNotes(): Boolean {
        return isShowAllNotes
    }

    fun setRefreshToken(refreshToken: String) {
        AuthManager.refreshToken = refreshToken
        sharedPref!!.edit { putString("REFRESH_TOKEN", refreshToken) }
    }

    fun getRefreshToken(): String {
        return refreshToken ?: ""
    }

    @JvmStatic
    fun setEmailLogin(
        email: String, username: String, token: String, refreshToken: String, isVerified: Boolean
    ) {
        type = AuthType.EMAIL
        AuthManager.email = email
        AuthManager.username = username
        AuthManager.isVerified = isVerified
        setToken(token)
        setRefreshToken(refreshToken)
        CoroutineScope(Dispatchers.Main).launch {
            if (onAuthChangeHandler != null) onAuthChangeHandler!!.updateAvatar(getAvatar())
        }
        sharedPref!!.edit {
            putInt("TYPE", 1)
            putString("EMAIL", email)
            putString("NAME", username)
            putBoolean("VERIFY", isVerified)
        }
        Notes.getOnNoteChangeHandler()?.onUserChanged()
    }

    @JvmStatic
    fun setGoogleLogin(): Boolean {
        type = AuthType.GOOGLE
        username = ""
        email = Clerk.user?.emailAddresses?.get(0)?.emailAddress.toString()
        CoroutineScope(Dispatchers.Main).launch {
            if (onAuthChangeHandler != null) onAuthChangeHandler!!.updateAvatar(getAvatar())
        }
        sharedPref!!.edit {
            putInt("TYPE", 2)
            putString("NAME", "")
            putString("EMAIL", email)
        }
        Notes.getOnNoteChangeHandler()?.onUserChanged()
        return true
    }

    @JvmStatic
    fun setLogout() {
        type = AuthType.NULL
        email = ""
        username = ""
        token = ""
        refreshToken = ""
        CoroutineScope(Dispatchers.Main).launch {
            if (onAuthChangeHandler != null) onAuthChangeHandler!!.updateAvatar(getAvatar())
        }
        sharedPref!!.edit {
            putInt("TYPE", 0)
            putString("EMAIL", "")
            putString("NAME", "")
            putString("TOKEN", "")
            putBoolean("VERIFY", false)
            putString("REFRESH_TOKEN", "")
        }
        Notes.getOnNoteChangeHandler()?.onUserChanged()
    }

    @JvmStatic
    fun getUsername(): String? {
        if (type == AuthType.NULL) return null
        return username
    }

    @JvmStatic
    fun setUsername(username: String) {
        this.username = username
        sharedPref!!.edit { putString("NAME", username) }

        CoroutineScope(Dispatchers.Main).launch {
            if (onAuthChangeHandler != null) onAuthChangeHandler!!.updateAvatar(
                getAvatar()
            )
        }
    }

    @JvmStatic
    fun getIsVerified(): Boolean {
        return type != AuthType.EMAIL || isVerified
    }

    @JvmStatic
    fun setIsVerified(isVerified: Boolean) {
        this.isVerified = isVerified
        sharedPref!!.edit { putBoolean("VERIFY", isVerified) }
    }

    @JvmStatic
    fun getEmail(): String? {
        if (type == AuthType.NULL) return null
        return email
    }

    @JvmStatic
    fun setEmail(email: String) {
        this.email = email
        sharedPref!!.edit { putString("EMAIL", email) }
    }

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
                        128f,
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
                Glide.with(getInstance().getContext()).asBitmap().load(url)
                    .apply(RequestOptions.circleCropTransform()).submit().get()
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun saveClerkAvatarToFile(bitmap: Bitmap) {
        Executors.newSingleThreadExecutor().execute {
            try {
                val folder = File(getInstance().getContext().filesDir, "avatar")
                if (!folder.exists()) folder.mkdirs()

                val file = File(getInstance().getContext().filesDir, "google.png")
                if (!file.exists()) file.createNewFile()

                FileOutputStream(file).use { out ->
                    bitmap.compress(
                        Bitmap.CompressFormat.PNG, 100, out
                    )
                }
            } catch (e: Exception) {
                Message.debug("Cannot save avatar: $e")
            }
        }
    }

    private fun getClerkAvatarFromFile(): Bitmap? {
        val file = File(getInstance().getContext().filesDir, "google.png")
        if (file.exists()) return BitmapFactory.decodeFile(file.absolutePath)
        return null
    }

    @JvmStatic
    val isLogin: Boolean
        get() = Clerk.user != null

    @JvmStatic
    fun getType(): AuthType? {
        return type
    }

    suspend fun emailLogin(email: String, password: String): Boolean {
        val apiReq = ApiReq(email = email, password = password)
        try {
            val response = RetrofitInstance.api.emailLogin(apiReq)
            if (response.isSuccessful) {
                val res = response.body()
                if (res?.success == true && res.user?.name != null) {
                    val token = response.headers()["Authorization"] ?: ""
                    val refreshToken = response.headers()["RefreshToken"] ?: ""
                    setEmailLogin(
                        res.user.email, res.user.name, token, refreshToken, res.user.isVerified
                    )
                    Notes.updateNote(res.user.notes)
                    return true
                }
                return false
            } else {
                ErrorHandler.res(response)
                return false
            }
        } catch (_: Exception) {
            Message.cannotReachServer()
            return false
        }
    }

    suspend fun emailLogout(): Boolean {
        if (type != AuthType.EMAIL) return false
        try {
            val response = RetrofitInstance.api.emailLogout()
            if (response.isSuccessful) {
                val res = response.body()
                if (res?.success == true) {
                    setLogout()
                    return true
                }
                return false
            } else {
                ErrorHandler.res(response)
                return false
            }
        } catch (_: Exception) {
            Message.cannotReachServer()
            return false
        }
    }

    suspend fun checkAuth(): Boolean {
        try {
            ClerkAuth.getToken()
            val response = RetrofitInstance.api.checkAuth()
            if (response.isSuccessful) {
                val res = response.body()
                if (res?.success == true && res.user != null) {
                    Notes.updateNote(res.user.notes)
                    if (type == AuthType.GOOGLE) {
                        setEmail(Clerk.user?.emailAddresses?.get(0)?.emailAddress.toString())
                    } else {
                        setEmail(res.user.email)
                    }
                    setUsername(res.user.name)
                    setIsVerified(res.user.isVerified)
                    return true
                }
                return false
            } else {
                ErrorHandler.res(response)
                return false
            }
        } catch (_: Exception) {
            Message.cannotReachServer()
            return false
        }
    }

    suspend fun emailSignup(name: String, email: String, password: String): Boolean {
        val apiReq = ApiReq(name = name, email = email, password = password)
        try {
            val response = RetrofitInstance.api.signup(apiReq)
            if (response.isSuccessful) {
                val res = response.body()
                if (res?.success == true && res.user?.name != null) {
                    val token = response.headers()["Authorization"] ?: ""
                    val refreshToken = response.headers()["RefreshToken"] ?: ""
                    setEmailLogin(
                        res.user.email, res.user.name, token, refreshToken, res.user.isVerified
                    )
                    Notes.updateNote(res.user.notes)
                    return true
                }
                return false
            } else {
                if (response.code() == 409) {
                    Message.userExists()
                } else {
                    ErrorHandler.res(response)
                }
                return false
            }
        } catch (_: Exception) {
            Message.cannotReachServer()
            return false
        }
    }

    suspend fun changeUsername(name: String): Boolean {
        if (type != AuthType.EMAIL) return false
        val apiReq = ApiReq(name = name)
        try {
            val response = RetrofitInstance.api.changeUsername(apiReq)
            if (response.isSuccessful) {
                val res = response.body()
                if (res?.success == true) {
                    setUsername(name)
                    Message.show(
                        getInstance().getContext().getString(R.string.username_updated)
                    )
                    return true
                }
                return false
            } else {
                if (response.code() == 403) {
                    Message.usernameChangeDisable()
                } else {
                    ErrorHandler.res(response)
                }
                return false
            }
        } catch (_: Exception) {
            Message.cannotReachServer()
            return false
        }
    }

    suspend fun changePassword(oldPassword: String, newPassword: String): Boolean {
        if (type != AuthType.EMAIL) return false
        val apiReq = ApiReq(oldPassword = oldPassword, newPassword = newPassword)
        try {
            val response = RetrofitInstance.api.changePassword(apiReq)
            if (response.isSuccessful) {
                val res = response.body()
                if (res?.success == true) {
                    Message.show(
                        getInstance().getContext().getString(R.string.password_updated)
                    )
                    return true
                }
                return false
            } else {
                if (response.code() == 403) {
                    Message.passwordChangeDisable()
                } else {
                    ErrorHandler.res(response)
                }
                return false
            }
        } catch (_: Exception) {
            Message.cannotReachServer()
            return false
        }
    }

    suspend fun verifyEmail(code: String): Boolean {
        if (type != AuthType.EMAIL) return false
        val apiReq = ApiReq(code = code)
        try {
            val response = RetrofitInstance.api.verifyEmail(apiReq)
            if (response.isSuccessful) {
                val res = response.body()
                if (res?.success == true) {
                    Message.show(getInstance().getContext().getString(R.string.email_verified))
                    setIsVerified(true)
                    return true
                }
                return false
            } else {
                if (response.code() == 403) {
                    Message.invalidVerificationCode()
                } else {
                    ErrorHandler.res(response)
                }
                return false
            }
        } catch (_: Exception) {
            Message.cannotReachServer()
            return false
        }
    }

    suspend fun resendVerifyEmail(): Boolean {
        if (type != AuthType.EMAIL) return false
        try {
            val response = RetrofitInstance.api.resendVerifyEmail()
            if (response.isSuccessful) {
                val res = response.body()
                if (res?.success == true) {
                    Message.show(getInstance().getContext().getString(R.string.code_resent))
                    return true
                }
                return false
            } else {
                if (response.code() == 403) {
                    Message.unableVerifyEmail()
                } else {
                    ErrorHandler.res(response)
                }
                return false
            }
        } catch (_: Exception) {
            Message.cannotReachServer()
            return false
        }
    }

    suspend fun forgotPassword(email: String): Boolean {
        val apiReq = ApiReq(email = email)
        try {
            val response = RetrofitInstance.api.forgotPassword(apiReq)
            if (response.isSuccessful) {
                val res = response.body()
                if (res?.success == true) {
                    Message.show(getInstance().getContext().getString(R.string.sent_reset_password_code))
                    return true
                }
                return false
            } else {
                ErrorHandler.res(response)
                return false
            }
        } catch (_: Exception) {
            Message.cannotReachServer()
            return false
        }
    }

    @JvmStatic
    fun getUserId(): String {
        if (type == AuthType.NULL) return ""
        return "$email#$type"
    }

    @JvmStatic
    fun isShowNote(userId: String): Boolean {
        return isShowAllNotes || userId.isEmpty() || userId == getUserId()
    }

    @JvmStatic
    fun isOwner(userId: String): Boolean {
        return userId.isEmpty() || userId == getUserId()
    }
}
