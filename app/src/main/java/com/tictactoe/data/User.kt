package com.tictactoe.data

data class User( val id: String,val name: String?,val status: UserOnlineStatus= USER_STATUS_ONLINE)

typealias UserOnlineStatus=Int
const val USER_STATUS_ONLINE = 0
const val USER_STATUS_OFFLINE = 1