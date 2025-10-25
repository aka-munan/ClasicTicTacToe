package com.tictactoe.domain.models

data class MatchInstance (
    val createdAt: Long,
    val player1: String,
    val player2: String,
    val winner: String,
    val opponentName: String?
)