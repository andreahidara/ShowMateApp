package com.andrea.showmateapp.data.model

data class SharedList(
    val listId: String = "",
    val listName: String = "",
    val ownerUid: String = "",
    val ownerUsername: String = "",
    val memberUids: List<String> = emptyList(),
    val memberUsernames: List<String> = emptyList(),
    val showIds: List<Long> = emptyList(),
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L
)
