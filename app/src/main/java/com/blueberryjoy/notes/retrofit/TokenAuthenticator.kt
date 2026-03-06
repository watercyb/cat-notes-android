package com.blueberryjoy.notes.retrofit

import com.blueberryjoy.notes.auth.AuthManager
import com.blueberryjoy.notes.retrofit.data_class.ApiReq
import com.blueberryjoy.notes.utils.Message
import kotlinx.coroutines.runBlocking
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route

class TokenAuthenticator : Authenticator {

    override fun authenticate(route: Route?, response: Response): Request? {
        if (responseCount(response) >= 2) {
            return null
        }

        synchronized(this) {
            val newToken = refreshToken()

            if (newToken.isEmpty()) return null

            return response.request.newBuilder()
                .header("Authorization", newToken)
                .build()
        }
    }

    private fun refreshToken(): String {
        val apiReq = ApiReq(refreshToken = AuthManager.getRefreshToken())
        val response = runBlocking {
            RetrofitInstance.refresh.refresh(apiReq)
        }
        if (response.isSuccessful) {
            val token = response.headers()["Authorization"] ?: ""
            val refreshToken = response.headers()["RefreshToken"] ?: ""
            AuthManager.setToken(token)
            AuthManager.setRefreshToken(refreshToken)
            Message.showFromThread("New Token")
            return token
        }
        return ""
    }

    private fun responseCount(response: Response): Int {
        var res = response
        var count = 1
        while (res.priorResponse != null) {
            count++
            res = res.priorResponse!!
        }
        return count
    }
}