package com.andrea.showmateapp.data.model

data class FriendRequest(
    val id: String = "",
    val fromUid: String = "",
    val toUid: String = "",
    val fromUsername: String = "",
    val toUsername: String = "",
    val status: String = STATUS_PENDING,
    val createdAt: Long = 0L
) {
    companion object {
        const val STATUS_PENDING = "pending"
        const val STATUS_ACCEPTED = "accepted"
    }
}
