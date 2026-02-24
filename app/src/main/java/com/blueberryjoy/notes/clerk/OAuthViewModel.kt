package com.blueberryjoy.notes.clerk

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.blueberryjoy.notes.auth.AuthManager
import com.blueberryjoy.notes.message.Message
import com.blueberryjoy.notes.utils.AuthType
import com.clerk.api.Clerk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

class OAuthViewModel : ViewModel() {
    private val _uiState = MutableStateFlow<UiState>(UiState.Loading)
    val uiState = _uiState.asStateFlow()

    init {
        combine(Clerk.isInitialized, Clerk.userFlow) { isInitialized, user ->
            when {
                !isInitialized -> UiState.Loading
                user != null -> UiState.Authenticated
                else -> UiState.SignedOut
            } to user  // return a Pair<UiState, User?>
        }.onEach { (state, user) ->
            Message.logW("Clerk user: $user state: $state")

            if (state != UiState.Loading) {
                if (user != null) {
                    AuthManager.setGoogleLogin()
                } else if (AuthManager.getType() == AuthType.GOOGLE) {
                    AuthManager.setLogout()
                }
            }

            _uiState.value = state
        }.launchIn(viewModelScope)
    }

    sealed interface UiState {
        data object Loading : UiState
        data object SignedOut : UiState
        data object Authenticated : UiState
    }
}