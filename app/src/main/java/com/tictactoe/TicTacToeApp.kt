package com.tictactoe

import android.app.Application
import com.google.firebase.FirebaseApp

class TicTacToeApp: Application() {
    override fun onCreate() {
        super.onCreate()
        FirebaseApp.initializeApp(this)
    }
}