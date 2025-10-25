package com.tictactoe.ui.home

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.database.database
import com.google.firebase.firestore.Filter
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.firestore
import com.tictactoe.R
import com.tictactoe.data.User
import com.tictactoe.domain.models.AuthState
import com.tictactoe.domain.models.MatchInstance
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class HomeViewModel : ViewModel() {
    private val firestore = Firebase.firestore
    private val userDb = Firebase.database.getReference("users")
    private val auth = Firebase.auth
    private val _authState = MutableLiveData<AuthState>().apply {
        value = AuthState.Loading
    }
    val authState: LiveData<AuthState> = _authState
    private val TAG = "HomeViewModel"

    private val _matchHistory = MutableLiveData<List<MatchInstance>>(listOf())
    val matchHistory : LiveData<List<MatchInstance>> = _matchHistory
    private val scope = CoroutineScope(Dispatchers.IO)

    init {
        CoroutineScope(Dispatchers.IO).launch {
            val user = getUser()
            withContext(Dispatchers.Main) {
                if (user == null)
                    _authState.postValue(AuthState.Unauthenticated)
                else
                    _authState.postValue(AuthState.Authenticated(user))
            }
        }
    }

    private suspend fun getUser(): User? {
        val uid = auth.currentUser?.uid ?: return null
        return getUserFromDb(uid)
    }

     fun getPlayerHistory() {
        val state = authState.value as? AuthState.Authenticated ?: return
        firestore.collection("matches").where(
            Filter.or(
                Filter.equalTo("player1", state.user.id),
                Filter.equalTo("player2", state.user.id)
            )
        ).orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(20)
            .get().addOnSuccessListener {
                scope.launch {
                   val matches = it.documents.map {
                        val player1 = it.getString("player1")!!
                        val player2 = it.getString("player2")!!
                        val opponentId = if (player1 == state.user.id) player2 else player1
                       val opponentName =  getUserFromDb(opponentId).name
                        MatchInstance(
                            it.getLong("createdAt")!!,
                            it.getString("player1")!!,
                            it.getString("player2")!!,
                            it.getString("winner")!!,
                            opponentName
                        )
                    }
                    viewModelScope.launch {
                        _matchHistory.postValue(matches)
                    }
                }
            }.addOnFailureListener {
                Log.e(TAG, "getPlayerHistory: ", it)
            }
    }

    private suspend fun getUserFromDb(uid: String): User {
        val result = userDb.child(uid).get().addOnSuccessListener {
        }.await()
        val user = User(
            result.key as String,
            result.child("name").value as String?,
            1
        )
        return user
    }
}