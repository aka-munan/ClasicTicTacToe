package com.tictactoe.data.repository

import android.util.Log
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.TaskCompletionSource
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.userProfileChangeRequest
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.tictactoe.data.USER_STATUS_OFFLINE
import com.tictactoe.data.USER_STATUS_ONLINE
import com.tictactoe.data.User
import kotlinx.coroutines.tasks.await

class AuthRepo {

    private val auth = FirebaseAuth.getInstance()
    private val TAG = "AuthRepo"
    private val rtdb = FirebaseDatabase.getInstance()

    suspend fun signInAnonymously(): String? {
        val result = auth.signInAnonymously().addOnCompleteListener {
            if (it.isSuccessful) {
                Log.d(TAG, "signInAnonymously:success")
            } else {
                Log.w(TAG, "signInAnonymously:failure", it.exception)
            }
        }
        result.continueWith { updateUserDb() }.await()
        return result.result.user?.uid
    }
     fun isSignedIn(): Boolean {
        val user = auth.currentUser
        return user != null
    }

    fun getCurrentUser(): User? {
        val user = auth.currentUser ?: return null
        return User(user.uid, user.displayName, 1)
    }

    private fun setOnlinePresence() {
        val statusRef = rtdb.getReference("users/${getCurrentUser()?.id}/status")
        statusRef.onDisconnect().setValue(USER_STATUS_OFFLINE)
        val connectedRef = rtdb.getReference(".info/connected")
        connectedRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val connected = snapshot.getValue(Boolean::class.java) ?: false
                if (connected) {
                    statusRef.setValue(USER_STATUS_ONLINE)
                    Log.d(TAG, "connected")
                } else {
                    Log.d(TAG, "not connected")
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.w(TAG, "Listener was cancelled")
            }
        })
    }

    suspend fun changeName(name: String) {
        val user = getCurrentUser() ?: return
        val userRef = rtdb.getReference("users/${user.id}")
        val updateProfileReq = userProfileChangeRequest {
            displayName = name
        }
        auth.currentUser?.updateProfile(updateProfileReq)?.continueWith {
            userRef.child("name").setValue(name).addOnCompleteListener {
                if (it.isSuccessful) {
                    Log.d(TAG, "name changed")
                } else {
                    Log.d(TAG, "name not changed")
                }
            }
        }?.await()
    }
    private  fun updateUserDb(): Task<Void?> {
        val fbUser = getCurrentUser() ?: return TaskCompletionSource<Void?>().task
        val userRef = rtdb.getReference("users/${fbUser.id}")
        val user = User(fbUser.id, fbUser.name, USER_STATUS_ONLINE)
        return userRef.setValue(user).addOnCompleteListener {
            if (it.isSuccessful) {
                Log.d(TAG, "user db updated")
            } else {
                Log.d(TAG, "user db not updated")
            }
        }
    }
}
