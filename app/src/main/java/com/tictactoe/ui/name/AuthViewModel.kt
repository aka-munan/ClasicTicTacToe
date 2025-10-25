package com.tictactoe.ui.name

import androidx.lifecycle.ViewModel
import androidx.lifecycle.liveData
import androidx.lifecycle.viewModelScope
import com.tictactoe.data.repository.AuthRepo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch

sealed class ChangeNameResult {
    object InProgress : ChangeNameResult()
    object Success : ChangeNameResult()
    data class Error(val message: String) : ChangeNameResult()
}

class AuthViewModel(private val authRepo: AuthRepo) : ViewModel() {

    fun changeName(name: String) =liveData{
        if (name.isBlank()) {
            emit(ChangeNameResult.Error("Name cannot be blank"))
            return@liveData
        }
        emit(ChangeNameResult.InProgress)
        try {
            authRepo.changeName(name)
            emit(ChangeNameResult.Success)
        } catch (e: Exception) {
            emit(ChangeNameResult.Error(e.message ?: "An unknown error occurred"))
        }
    }

    fun signInAnonymously() = liveData {
        emit(authRepo.signInAnonymously())
    }


    fun isSignedIn() = authRepo.isSignedIn()


}