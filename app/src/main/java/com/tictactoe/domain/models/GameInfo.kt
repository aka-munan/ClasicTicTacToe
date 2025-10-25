package com.tictactoe.domain.models

import com.tictactoe.data.User

data class GameInfo(
    val gameId: String,
    val opponent: User,
    val currentPlayer: Int,
    val status: String,
)
