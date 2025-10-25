package com.tictactoe.ui.game

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tictactoe.data.repository.MatchRepo
import com.tictactoe.domain.models.GameInfo
import com.tictactoe.domain.models.GameState
import com.tictactoe.domain.models.LocalPlayingGameState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PlayOnlineViewModel : ViewModel() {
    private val matchRepo = MatchRepo()

    var gameInfo: GameInfo? = null
        private set
    var gridSize: Int = 3
    private val _matrix = MutableLiveData<Array<Array<Int>>>(Array(gridSize) {
        Array(gridSize) { -1 }
    })
    val matrix: LiveData<Array<Array<Int>>> = _matrix
    private val _gameState =
        MutableLiveData<GameState>(GameState.Searching(System.currentTimeMillis()))
    val gameState: LiveData<GameState> = _gameState
    private val scope = CoroutineScope(Dispatchers.IO)

    init {
        findMatch()
    }

    fun findMatch() {
        viewModelScope.launch {
            _gameState.value = GameState.Searching(System.currentTimeMillis())
            matchRepo.findMatch(onMatchFound = { gameInfo ->
                this@PlayOnlineViewModel.gameInfo = gameInfo
                _gameState.value = GameState.Matched(gameInfo.gameId, gameInfo.opponent)
                _gameState.value =
                    GameState.XTurn(gameInfo.gameId)
                listenForGameUpdates(gameInfo.gameId)
            })
        }
    }

    fun toggleTurn(x: Int, y: Int) {//returns currnet player 0 for o,1 for X
        gameInfo ?: run {
            _gameState.value = GameState.Error("Game not found")
            return
        }
        _gameState.value =
            if (gameInfo!!.currentPlayer == 0) GameState.XTurn(gameInfo!!.gameId) else GameState.OTurn(
                gameInfo!!.gameId
            )
        scope.launch {
            matchRepo.setEvent(gameInfo!!.gameId, x, y).onSuccess {
                val newMatrix = _matrix.value!!.copyOf()
                newMatrix[x][y] = gameInfo!!.currentPlayer
                withContext(Dispatchers.Main) {
                    _matrix.postValue(newMatrix)
                    val winner = getWinner(matrix.value!!)
                    if (winner != null) {
                        if (winner == 0) {
                            matchRepo.saveToFirestore(
                                gameInfo!!.gameId,
                                if (gameInfo!!.currentPlayer == 0) matchRepo.getUid() else gameInfo!!.opponent.id
                            )
                            _gameState.value = GameState.Finished(gameInfo!!.gameId, "O Won")
                        } else {
                            matchRepo.saveToFirestore(
                                gameInfo!!.gameId,
                                if (gameInfo!!.currentPlayer == 1) matchRepo.getUid() else gameInfo!!.opponent.id
                            )
                            _gameState.value = GameState.Finished(gameInfo!!.gameId, "X Won")
                        }
//                    return
                    }
                    if (checkDraw()) {
                        matchRepo.saveToFirestore(
                            gameInfo!!.gameId,
                            "draw"
                        )
                        _gameState.value = GameState.Finished(gameInfo!!.gameId, "Game Draw")
//                    return player
                    }
                }
            }
        }
        return
    }

    private fun listenForGameUpdates(gameId: String) {
        matchRepo.listenToGameUpdates(gameId) { event ->
            val newMatrix = _matrix.value!!.copyOf()
            newMatrix[event.x][event.y] = if (gameInfo!!.currentPlayer == 0) 1 else 0
            _matrix.postValue(newMatrix)
            val winner = getWinner(matrix.value!!)
            if (winner != null) {
                if (winner == 0) {
                    matchRepo.saveToFirestore(
                        gameInfo!!.gameId,
                        if (gameInfo!!.currentPlayer == 0) matchRepo.getUid() else gameInfo!!.opponent.id
                    )
                    _gameState.value = GameState.Finished(gameInfo!!.gameId, "O Won")
                } else {
                    matchRepo.saveToFirestore(
                        gameInfo!!.gameId,
                        if (gameInfo!!.currentPlayer == 1) matchRepo.getUid() else gameInfo!!.opponent.id
                    )
                    _gameState.value = GameState.Finished(gameInfo!!.gameId, "X Won")
                }
                return@listenToGameUpdates
            }
            if (checkDraw()) {
                matchRepo.saveToFirestore(
                    gameInfo!!.gameId,
                    "draw"
                )
                _gameState.value = GameState.Finished(gameInfo!!.gameId, "Game Draw")
                return@listenToGameUpdates
            }
            _gameState.value =
                if (gameInfo!!.currentPlayer == 1) GameState.XTurn(gameInfo!!.gameId) else GameState.OTurn(
                    gameInfo!!.gameId
                )
        }
    }

    fun newGame() {
        _matrix.value = Array(gridSize) {
            Array(gridSize) { -1 }
        }
        _gameState.value = GameState.Searching(System.currentTimeMillis())
        gameInfo = null
        findMatch()
    }

    fun removeListener() {
        matchRepo.removeListener(gameInfo!!.gameId)
    }

    private fun checkDraw(): Boolean {
        _matrix.value!!.forEach {
            it.forEach {
                if (it == -1) {
                    return false
                }
            }
        }
        return true
    }
}
