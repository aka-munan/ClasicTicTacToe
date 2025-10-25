package com.tictactoe.domain.models

import java.util.Date

data class GameEvent(
    val uid: String,
    val time: Date?,
    val x: Int, val y: Int,
)
