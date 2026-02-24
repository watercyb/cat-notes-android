package com.blueberryjoy.notes.clerk

import android.util.Log
import com.blueberryjoy.notes.auth.AuthManager
import com.blueberryjoy.notes.auth.AuthManager.setLogout
import com.blueberryjoy.notes.message.Message
import com.clerk.api.Clerk
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

    suspend fun login(provider: OAuthProvider) {
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
                            Message.showFromThread("Signed in")
                        } else {
                            // If the status is not complete, check why. User may need to
                            // complete further steps.
                            Message.showErrorFromThread("Cannot sign in")
                        }
                    }

                    ResultType.SIGN_UP -> {
                        // The OAuth flow resulted in a sign-up
                        if (it.signUp?.status == SignUp.Status.COMPLETE) {
                            Message.showFromThread("Signed uo")
                        } else {
                            // If the status is not complete, check why. User may need to
                            // complete further steps.
                            Message.showErrorFromThread("Cannot sign up")
                        }
                    }

                    else -> {
                        Message.showErrorFromThread("OAuthViewModel error")
                    }
                }
            }.onFailure {
                // See https://clerk.com/docs/guides/development/custom-flows/error-handling
                // for more info on error handling
                Message.showErrorFromThread("OAuthViewModel error: $it.errorMessage")
            }
    }

    suspend fun logout(): Boolean {
        Clerk.signOut()
            .onSuccess {
                Log.w("testLog", "Logout")
                setLogout()
                return true
            }
            .onFailure {
                Log.e("testLog", it.errorMessage, it.throwable)
                return false
            }
        return true
    }
}