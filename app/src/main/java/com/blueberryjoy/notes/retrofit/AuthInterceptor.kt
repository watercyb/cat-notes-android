package com.blueberryjoy.notes.retrofit

import com.blueberryjoy.notes.BuildConfig
import com.blueberryjoy.notes.auth.AuthManager
import okhttp3.Interceptor
import okhttp3.Response

class AuthInterceptor() : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request().newBuilder()
            .apply {
                val token = AuthManager.getToken()
                if (token != null) {
                    addHeader("Authorization", token)
                }
                addHeader("X-Client-Type", "android-app")
                addHeader("X-App-Version", BuildConfig.VERSION_NAME)
                addHeader("Content-Type", "application/json")
            }
            .build()

        return chain.proceed(request)
    }
}