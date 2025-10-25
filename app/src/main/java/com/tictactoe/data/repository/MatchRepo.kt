package com.tictactoe.data.repository

import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ServerValue
import com.google.firebase.database.ValueEventListener
import com.google.firebase.firestore.firestore
import com.tictactoe.data.User
import com.tictactoe.domain.models.GameEvent
import com.tictactoe.domain.models.GameInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.time.Instant
import java.util.Date

class MatchRepo {
    private lateinit var listener: ChildEventListener
    private val firebaseDatabase = FirebaseDatabase.getInstance()
    private val gameDb = firebaseDatabase.getReference("game")
    private val userDb = firebaseDatabase.getReference("users")
    private val firestore = Firebase.firestore
    private val auth = FirebaseAuth.getInstance()
    private val scope = CoroutineScope(Dispatchers.IO)
    private val TAG = "MatchRepo"

    suspend fun findMatch(onMatchFound: (GameInfo) -> Unit) {
        val queueRef = gameDb.child("queue")
        val snapshot = queueRef.limitToFirst(10).get().await()
        val uid = getUid()

        // find someone waiting
        val queueInstanse = snapshot.children.firstOrNull {
            it.key != uid
        }

        val opponent = queueInstanse?.key

        if (opponent != null) {
            // remove opponent from queue
            Log.i(TAG, "match instance: $queueInstanse")
            queueRef.child(opponent).removeValue()
            val matchInfo = createMatch(queueInstanse.child("matchId").value as String, opponent)
            onMatchFound(matchInfo)
            matchInfo
        } else {
            // no one waiting, add current user to queue
            val data = mapOf(
                "timestamp" to ServerValue.TIMESTAMP,
                "matchId" to queueRef.push().key
            )
            queueRef.child(uid).setValue(data)
            waitForMatch(data["matchId"] as String, onMatchFound)
        }
    }

    private suspend fun createMatch(matchId: String, opponentUid: String): GameInfo {
        val uid = getUid()
        val matchData = mapOf(
            "player1" to uid,
            "player2" to opponentUid,
            "status" to "active",
            "createdAt" to ServerValue.TIMESTAMP
        )
        gameDb.child("matches").child(matchId).setValue(matchData).await()
        val user = getUserFromDb(opponentUid)
        return GameInfo(matchId, user!!, 0, "active")
    }

    private fun waitForMatch(matchId: String, onMatchFound: (GameInfo) -> Unit) {
        val uid = getUid()
        val matchesRef = gameDb.child("matches/$matchId")
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                Log.i(TAG, "onDataChange: $snapshot")
                snapshot.let { match ->
                    val player1 = match.child("player1").value
                    val player2 = match.child("player2").value
                    if (player1 == uid || player2 == uid) {
                        val opponentUid =
                            if (player1 == uid) player2 as String else player1 as String
                        CoroutineScope(Dispatchers.IO).launch {
                            val opponent = getUserFromDb(opponentUid)
                            CoroutineScope(Dispatchers.Main).launch {
                                onMatchFound(GameInfo(match.key!!, opponent!!, 1, "active"))
                            }
                        }
                        matchesRef.removeEventListener(this)
//                        continuation.resume(Unit, null)
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Error listening for matches", error.toException())

                matchesRef.removeEventListener(this)
//                continuation.cancel(error.toException())
            }
        }
        matchesRef.addValueEventListener(listener)
    }

    private suspend fun getUserFromDb(uid: String): User? {
        val result = userDb.child(uid).get().addOnSuccessListener {
        }.await()
        val user = User(
            result.key as String,
            result.child("name").value as String?,
            1
        )
        return user
    }

    fun getUid(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User is not currently signed in")
    }

    fun listenToGameUpdates(matchId: String, onUpdate: (GameEvent) -> Unit) {
        val currentUserId = getUid()
        val currentRoomRef = gameDb.child("matches/$matchId/gameEvents")
        listener = currentRoomRef.addChildEventListener(object : ChildEventListener {
            override fun onChildAdded(
                snapshot: DataSnapshot,
                previousChildName: String?
            ) {
                snapshot.let { event ->
                    val gameEvent = GameEvent(
                        event.child("uid").value as String,
                        Date.from(Instant.ofEpochMilli(event.child("time").value as Long)),
                        (event.child("x").value as Long).toInt(),
                        (event.child("y").value as Long).toInt()
                    )
                    if (gameEvent.uid != currentUserId) {
                        Log.i("MatchRepo", "gameEvent: $snapshot")
                        onUpdate(gameEvent)
                    }
                }
            }

            override fun onChildChanged(
                snapshot: DataSnapshot,
                previousChildName: String?
            ) {
            }

            override fun onChildRemoved(snapshot: DataSnapshot) {
            }

            override fun onChildMoved(
                snapshot: DataSnapshot,
                previousChildName: String?
            ) {
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Error listening for game updates", error.toException())
            }

        })
    }

    fun removeListener(gameId: String) {
        gameDb.child(gameId).child("gameEvents").removeEventListener(listener)
    }

    suspend fun setEvent(matchId: String, x: Int, y: Int): Result<Unit> {
        val currentUserId = getUid()
        val currentRoomRef = gameDb.child("matches/$matchId/gameEvents")
        val event = mapOf(
            "uid" to currentUserId,
            "time" to ServerValue.TIMESTAMP,
            "x" to x,
            "y" to y
        )
        val result = currentRoomRef.push().setValue(event)
        result.await()
        return if (result.isSuccessful)
            Result.success(Unit)
        else
            Result.failure(result.exception!!)
    }

    fun saveToFirestore(matchId: String, winner: String) {
        val uid = getUid()
        val matchDocument = firestore.collection("matches").document(matchId)
        scope.launch {
            if (matchDocument.get().await().exists()) return@launch
            val matchDataResult =
                gameDb.child("matches/$matchId").get()
            matchDataResult.await()
            if (matchDataResult.isSuccessful){
                val dataSnapshot = matchDataResult.result
                val matchData = dataSnapshot.value as MutableMap<String, Any>
                matchData.remove("gameEvents")
                matchData["status"] = "finished"
                matchData["winner"] = winner
                matchDocument.set(matchData).continueWith {
                    val batch=firestore.batch()
                    dataSnapshot.child("gameEvents").children.forEach {
                        val eventsDoc= matchDocument.collection("events").document()
                        batch.set(eventsDoc,it.value as Map<*,*>)
                    }
                    batch.commit().addOnSuccessListener {
                        gameDb.child("matches/$matchId").removeValue()
                    }
                }
            }
        }
    }
}