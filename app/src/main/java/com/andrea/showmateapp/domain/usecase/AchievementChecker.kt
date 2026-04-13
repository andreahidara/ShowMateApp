package com.andrea.showmateapp.domain.usecase

import com.andrea.showmateapp.data.model.Achievement
import com.andrea.showmateapp.data.model.UserProfile
import com.andrea.showmateapp.domain.repository.IAchievementRepository
import com.andrea.showmateapp.domain.repository.ISocialRepository
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

@Singleton
class AchievementChecker @Inject constructor(
    private val repo: IAchievementRepository,
    private val social: ISocialRepository
) {
    private val _unlockEvents = MutableSharedFlow<Achievement>(extraBufferCapacity = 8)
    val unlockEvents = _unlockEvents.asSharedFlow()

    data class EvalContext(
        val profile: UserProfile,
        val episodesToday: Int = 0,
        val voteCount: Int? = null,
        val countries: List<String>? = null,
        val reviewCount: Int? = null,
        val maxReviewLikes: Int? = null
    )

    suspend fun evaluate(ctx: EvalContext) {
        val unlocked = repo.getUnlockedIds().toHashSet()
        val c = mutableListOf<String>()
        val p = ctx.profile

        if ("first_show" !in unlocked && (p.likedMediaIds.isNotEmpty() || p.watchedEpisodes.isNotEmpty())) c += "first_show"
        if ("genre_explorer" !in unlocked && p.genreScores.size >= 10) c += "genre_explorer"
        if ("korean_drama" !in unlocked && ctx.countries?.contains("KR") == true) c += "korean_drama"
        if ("marathon_day" !in unlocked && ctx.episodesToday >= 5) c += "marathon_day"
        if ("weekend_warrior" !in unlocked && hasWeekend(p.viewingHistory)) c += "weekend_warrior"

        ctx.reviewCount?.let {
            if ("first_review" !in unlocked && it >= 1) c += "first_review"
            if ("prolific_critic" !in unlocked && it >= 10) c += "prolific_critic"
        }
        if ("popular_review" !in unlocked && (ctx.maxReviewLikes ?: 0) >= 50) c += "popular_review"

        val fCount = social.getFriends().size
        if ("first_friend" !in unlocked && fCount >= 1) c += "first_friend"
        if ("social_butterfly" !in unlocked && fCount >= 5) c += "social_butterfly"
        if ("hidden_gem" !in unlocked && (ctx.voteCount ?: Int.MAX_VALUE) < 1000) c += "hidden_gem"

        emit(c, unlocked)
    }

    suspend fun onGroupMatchCompleted(completedMatchCount: Int) {
        if (completedMatchCount >= 3) emit(listOf("group_matcher"), repo.getUnlockedIds().toHashSet())
    }

    suspend fun addXp(delta: Int) = runCatching { repo.addXp(delta) }

    private suspend fun emit(ids: List<String>, unlocked: Set<String>) {
        val new = ids.filter { it !in unlocked }
        if (new.isEmpty()) return
        val xp = new.sumOf { AchievementDefs.byId[it]?.xpReward ?: 0 }
        repo.unlockAchievements(new, xp)
        val now = System.currentTimeMillis()
        new.forEach { id -> AchievementDefs.byId[id]?.copy(unlockedAt = now)?.let { _unlockEvents.tryEmit(it) } }
    }

    private fun hasWeekend(history: List<String>): Boolean {
        val fmt = DateTimeFormatter.ISO_LOCAL_DATE
        return history.mapNotNull { raw ->
            val p = raw.split(":")
            if (p.size < 2) return@mapNotNull null
            runCatching { LocalDate.parse(p[0], fmt) }.getOrNull()?.let { it to p[1] }
        }.filter { it.first.dayOfWeek.value >= 6 }
         .groupBy { "${it.second}_${it.first.year}W${it.first.dayOfYear / 7}" }
         .any { (_, es) -> es.map { it.first.dayOfWeek.value }.toSet().size >= 2 }
    }
}
