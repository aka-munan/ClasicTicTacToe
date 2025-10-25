package com.tictactoe.ui.game

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.tictactoe.domain.models.LocalPlayingGameState

class PlayLocalViewModel : ViewModel() {
    var gridSize: Int = 3
    private val _matrix = MutableLiveData<Array<Array<Int>>>(Array(gridSize) {
        Array(gridSize) { -1 }
    })
    val matrix: LiveData<Array<Array<Int>>> = _matrix
    private val _gameState = MutableLiveData<LocalPlayingGameState>(LocalPlayingGameState.XTurn)
    val gameState: LiveData<LocalPlayingGameState> = _gameState

    fun toggleTurn(x: Int, y: Int) :Int{//returns current player as 1== x, 0 == o
        val newMatrix = _matrix.value!!.copyOf()
        newMatrix[x][y] = when (_gameState.value) {
            LocalPlayingGameState.OTurn -> 0
            LocalPlayingGameState.XTurn -> 1
            else -> throw IllegalStateException("Game state is not togglable,${_gameState.value}")
        }
        _matrix.postValue(newMatrix)
        val winner =getWinner(matrix.value!!)
        if (winner != null)  {
            if (winner==0){
                _gameState.value = LocalPlayingGameState.Finished("O Won")
            }else{
                _gameState.value = LocalPlayingGameState.Finished("X Won")
            }
            return winner
        }
        if (checkDraw()){
            val player = if (_gameState.value == LocalPlayingGameState.OTurn) 0 else 1
            _gameState.value = LocalPlayingGameState.Finished("Game Draw")
            return player
        }
        _gameState.value = when (gameState.value) {
            LocalPlayingGameState.OTurn -> LocalPlayingGameState.XTurn
            LocalPlayingGameState.XTurn -> LocalPlayingGameState.OTurn
            else -> throw IllegalStateException("Game state is not togglable")
        }
        return if (_gameState.value == LocalPlayingGameState.OTurn) 1 else 0
    }

    private fun checkDraw(): Boolean{
        _matrix.value!!.forEach {
            it.forEach {
                if (it==-1){
                    return false
                }
            }
        }
        return true
    }



    fun newGame(){
        _matrix.value = Array(gridSize) {
            Array(gridSize) { -1 }
        }
        _gameState.value = LocalPlayingGameState.XTurn
    }
}
fun getWinner(board: Array<Array<Int>>): Int? {
    //check rows
    for (r in 0..2) {
        if (board[r][0] != -1 && board[r][0] == board[r][1] && board[r][1] == board[r][2]) {
            return board[r][0]
        }
    }
    // Check columns
    for (c in 0..2) {
        if (board[0][c] != -1 && board[0][c] == board[1][c] && board[1][c] == board[2][c]) {
            return board[0][c]
        }
    }
    // Check diagonal ↘
    for (i in 0 until board.size - 1) {
        if (board[i][i] == -1) break
        if (board[i][i] != board[i + 1][i + 1]) break
        if (i == board.size - 2) return board[i][i]
    }
    // Check diagonal ↗
    for (i in board.size - 1 downTo 1) {
        val j = board.size - 1 - i
        if (board[j][i] == -1) break
        if (board[j][i] != board[j + 1][i - 1]) break
        if (i == 1) return board[i][j]
    }
    return null
}