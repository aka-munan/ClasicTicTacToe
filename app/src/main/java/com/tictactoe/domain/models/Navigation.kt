package com.tictactoe.domain.models

sealed class Navigation {
    object Home: Navigation()
    object PlayLocal: Navigation()
    object PLayOnline: Navigation()
}