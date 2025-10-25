package com.tictactoe.ui.name

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.tictactoe.data.repository.AuthRepo

class AuthViewModelFactory(private val authRepo: AuthRepo) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AuthViewModel::class.java)) {
            return AuthViewModel(authRepo) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}