package com.tictactoe.domain.models

import com.tictactoe.data.User

sealed class GameState {
    data class Searching(val waitTime: Long=0) : GameState()
    data class Matched(val roomId: String, val opponent: User) : GameState()
    data class OTurn(val roomId: String) : GameState()
    data class XTurn(val roomId: String) : GameState()
    data class Finished(val roomId: String, val winner: String?) : GameState()
    data class Error(val message: String) : GameState()
}
