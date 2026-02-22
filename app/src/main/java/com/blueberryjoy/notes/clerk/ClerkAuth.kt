package com.blueberryjoy.notes.clerk

import android.util.Log
import com.blueberryjoy.notes.auth.AuthManager
import com.clerk.api.Clerk
import com.clerk.api.Clerk.signOut
import com.clerk.api.network.model.token.TokenResource
import com.clerk.api.network.serialization.ClerkResult
import com.clerk.api.network.serialization.errorMessage
import com.clerk.api.network.serialization.onFailure
import com.clerk.api.network.serialization.onSuccess
import com.clerk.api.session.fetchToken
import com.clerk.api.signin.SignIn
import com.clerk.api.signup.SignUp
import com.clerk.api.sso.OAuthProvider
import com.clerk.api.sso.ResultType

object ClerkAuth {
    suspend fun getToken(): Boolean {
        if (Clerk.session == null) return false
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

    suspend fun login(provider: OAuthProvider): String {
        SignIn.authenticateWithRedirect(
            SignIn.AuthenticateWithRedirectParams.OAuth(
                provider
            )
        )
            .onSuccess {
                when (it.resultType) {
                    ResultType.SIGN_IN -> {
                        // The OAuth flow resulted in a sign in
                        if (it.signIn?.status == SignIn.Status.COMPLETE) {
                            return "Signed in"
                        } else {
                            // If the status is not complete, check why. User may need to
                            // complete further steps.
                            return "Cannot sign in"
                        }
                    }

                    ResultType.SIGN_UP -> {
                        // The OAuth flow resulted in a sign-up
                        if (it.signUp?.status == SignUp.Status.COMPLETE) {
                            return "Signed up"
                        } else {
                            // If the status is not complete, check why. User may need to
                            // complete further steps.
                            return "Cannot sign up"
                        }
                    }

                    else -> {
                        return "Error"
                    }
                }
            }.onFailure {
                // See https://clerk.com/docs/guides/development/custom-flows/error-handling
                // for more info on error handling
                Log.e("OAuthViewModel error", it.errorMessage, it.throwable)
                return it.errorMessage
            }
        return "No response"
    }

    suspend fun logout() {
        Clerk.signOut()
            .onSuccess { Log.w("testLog", "Logout") }
            .onFailure {
                Log.e("testLog", it.errorMessage, it.throwable)
            }
    }
}