package com.tictactoe.domain.models

import com.tictactoe.data.User

sealed class AuthState {
    data class Authenticated(val user: User): AuthState()
    object Unauthenticated : AuthState()
    object Loading : AuthState()
}