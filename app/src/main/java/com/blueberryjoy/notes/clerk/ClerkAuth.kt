package com.blueberryjoy.notes.clerk

import com.blueberryjoy.notes.App
import com.blueberryjoy.notes.R
import com.blueberryjoy.notes.auth.AuthManager
import com.blueberryjoy.notes.auth.AuthManager.setLogout
import com.blueberryjoy.notes.utils.Message
import com.clerk.api.Clerk
import com.clerk.api.network.model.token.TokenResource
import com.clerk.api.network.serialization.ClerkResult
import com.clerk.api.network.serialization.onFailure
import com.clerk.api.network.serialization.onSuccess
import com.clerk.api.network.serialization.shortErrorMessageOrNull
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
                            Message.show(
                                App.getInstance().getContext().getString(R.string.google_signed_in)
                            )
                        } else {
                            // If the status is not complete, check why. User may need to
                            // complete further steps.
                            Message.show(
                                App.getInstance().getContext()
                                    .getString(R.string.unable_google_sign_in)
                            )
                        }
                    }

                    ResultType.SIGN_UP -> {
                        // The OAuth flow resulted in a sign-up
                        if (it.signUp?.status == SignUp.Status.COMPLETE) {
                            Message.debug("Signed up with Google: Should not be called")
                        } else {
                            // If the status is not complete, check why. User may need to
                            // complete further steps.
                            Message.debug("Cannot sign up: Should not be called")
                        }
                    }

                    else -> {
                        Message.show(
                            App.getInstance().getContext().getString(R.string.unable_google_sign_in)
                        )
                    }
                }
            }
            .onFailure { // See https://clerk.com/docs/guides/development/custom-flows/error-handling
                // for more info on error handling
                if (it.shortErrorMessageOrNull() == "Session already exists") {
                    AuthManager.setGoogleLogin()
                } else {
                    Message.show(
                        App.getInstance().getContext().getString(R.string.unable_google_sign_in)
                    )
                }
            }
    }

    suspend fun logout(): Boolean {
        Clerk.signOut()
            .onSuccess {
                setLogout()
                return true
            }
            .onFailure {
                Message.show(
                    App.getInstance().getContext().getString(R.string.unable_google_sign_out)
                )
                return false
            }
        return true
    }
}