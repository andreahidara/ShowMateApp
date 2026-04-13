package com.andrea.showmateapp.data.model

data class GroupSession(
    val id: String = "",
    val hostEmail: String = "",
    val memberEmails: List<String> = emptyList(),
    val candidateIds: List<Int> = emptyList(),
    val status: String = STATUS_LOBBY,
    val filters: GroupFilters = GroupFilters(),
    val vetoes: Map<String, Int> = emptyMap(),
    val matchedMediaId: Int = 0,
    val nightTitle: String = "",
    val createdAt: Long = 0L,
    val finishedAt: Long = 0L
) {
    companion object {
        const val STATUS_LOBBY = "lobby"
        const val STATUS_VOTING = "voting"
        const val STATUS_FINISHED = "finished"
    }
}

data class GroupFilters(
    val maxEpisodeDuration: Int = 0,
    val excludedGenreIds: List<Int> = emptyList()
)

data class MemberVoteDoc(
    val email: String = "",
    val yes: List<Int> = emptyList(),
    val no: List<Int> = emptyList(),
    val maybe: List<Int> = emptyList(),
    val superLikeId: Int = 0,
    val ready: Boolean = false
)

enum class VoteType { YES, NO, MAYBE, SUPER_LIKE }
