package com.tictactoe.domain.models

sealed class LocalPlayingGameState {
    object XTurn : LocalPlayingGameState()
    object OTurn : LocalPlayingGameState()
    data class Finished(val winner: String) : LocalPlayingGameState()
}