package com.blueberryjoy.notes.clerk

import com.blueberryjoy.notes.auth.AuthManager
import com.clerk.api.Clerk
import com.clerk.api.network.model.token.TokenResource
import com.clerk.api.network.serialization.ClerkResult
import com.clerk.api.network.serialization.onFailure
import com.clerk.api.network.serialization.onSuccess
import com.clerk.api.session.fetchToken

object ClerkAuth {
    suspend fun getToken(): Boolean {
        val session = Clerk.client.sessions.firstOrNull()
        if (session == null) {
            AuthManager.setToken(null)
            return false
        }
        val result: ClerkResult<TokenResource, *> = session.fetchToken()
        result.onSuccess { tokenResource ->
            val token = tokenResource.jwt
            AuthManager.setClerkToken(token)
            return true
        }.onFailure { error ->
            println("Failed to fetch token: ${error.error}")
            AuthManager.setClerkToken(null)
        }
        return false
    }

    fun getAvatar(): String {
        return Clerk.user?.imageUrl ?: ""
    }
}