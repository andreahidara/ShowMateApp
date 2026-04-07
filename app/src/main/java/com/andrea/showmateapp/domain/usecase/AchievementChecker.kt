package com.andrea.showmateapp.domain.usecase

import com.andrea.showmateapp.data.model.Achievement
import com.andrea.showmateapp.data.model.UserProfile
import com.andrea.showmateapp.domain.repository.IAchievementRepository
import com.andrea.showmateapp.domain.repository.ISocialRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AchievementChecker @Inject constructor(
    private val achievementRepository: IAchievementRepository,
    private val socialRepository: ISocialRepository,
) {

    private val _unlockEvents = MutableSharedFlow<Achievement>(extraBufferCapacity = 8)
    val unlockEvents: SharedFlow<Achievement> = _unlockEvents

    data class EvalContext(
        val profile: UserProfile,
        val episodesToday: Int = 0,
        val watchedShowVoteCount: Int? = null,
        val watchedShowOriginCountries: List<String>? = null,
        val reviewCount: Int? = null,
        val maxReviewLikes: Int? = null,
    )

    suspend fun evaluate(ctx: EvalContext) {
        val alreadyUnlocked = runCatching { achievementRepository.getUnlockedIds() }
            .getOrDefault(emptyList()).toHashSet()

        val candidates = mutableListOf<String>()
        val profile = ctx.profile

        if ("first_show" !in alreadyUnlocked &&
            (profile.likedMediaIds.isNotEmpty() || profile.watchedEpisodes.isNotEmpty())) {
            candidates += "first_show"
        }
        if ("genre_explorer" !in alreadyUnlocked && profile.genreScores.size >= 10) {
            candidates += "genre_explorer"
        }
        if ("korean_drama" !in alreadyUnlocked &&
            ctx.watchedShowOriginCountries?.contains("KR") == true) {
            candidates += "korean_drama"
        }

        if ("marathon_day" !in alreadyUnlocked && ctx.episodesToday >= 5) {
            candidates += "marathon_day"
        }
        if ("weekend_warrior" !in alreadyUnlocked && hasWeekendCompletion(profile.viewingHistory)) {
            candidates += "weekend_warrior"
        }

        ctx.reviewCount?.let { count ->
            if ("first_review"    !in alreadyUnlocked && count >= 1)  candidates += "first_review"
            if ("prolific_critic" !in alreadyUnlocked && count >= 10) candidates += "prolific_critic"
        }
        if ("popular_review" !in alreadyUnlocked && (ctx.maxReviewLikes ?: 0) >= 50) {
            candidates += "popular_review"
        }

        val friendCount = runCatching { socialRepository.getFriends().size }.getOrDefault(0)
        if ("first_friend"     !in alreadyUnlocked && friendCount >= 1) candidates += "first_friend"
        if ("social_butterfly" !in alreadyUnlocked && friendCount >= 5) candidates += "social_butterfly"

        if ("hidden_gem" !in alreadyUnlocked &&
            (ctx.watchedShowVoteCount ?: Int.MAX_VALUE) < 1000) {
            candidates += "hidden_gem"
        }

        emitUnlocks(candidates, alreadyUnlocked)
    }

    suspend fun onGroupMatchCompleted(completedMatchCount: Int) {
        if (completedMatchCount < 3) return
        val alreadyUnlocked = runCatching { achievementRepository.getUnlockedIds() }
            .getOrDefault(emptyList()).toHashSet()
        emitUnlocks(listOf("group_matcher"), alreadyUnlocked)
    }

    suspend fun addXp(delta: Int) {
        runCatching { achievementRepository.addXp(delta) }
    }

    private suspend fun emitUnlocks(candidates: List<String>, alreadyUnlocked: Set<String>) {
        val newIds = candidates.filter { it !in alreadyUnlocked }
        if (newIds.isEmpty()) return
        val totalXp = newIds.sumOf { AchievementDefs.byId[it]?.xpReward ?: 0 }
        runCatching { achievementRepository.unlockAchievements(newIds, totalXp) }
        val now = System.currentTimeMillis()
        newIds.forEach { id ->
            AchievementDefs.byId[id]?.copy(unlockedAt = now)?.let { _unlockEvents.tryEmit(it) }
        }
    }

    private fun hasWeekendCompletion(viewingHistory: List<String>): Boolean {
        val fmt = DateTimeFormatter.ISO_LOCAL_DATE
        data class Entry(val date: LocalDate, val showId: String)
        val weekendEntries = viewingHistory.mapNotNull { raw ->
            val parts = raw.split(":")
            if (parts.size >= 2) runCatching {
                val date = LocalDate.parse(parts[0], fmt)
                val dow = date.dayOfWeek.value
                if (dow == 6 || dow == 7) Entry(date, parts[1]) else null
            }.getOrNull() else null
        }
        val byShowWeek = weekendEntries.groupBy { "${it.showId}_${it.date.year}W${it.date.dayOfYear / 7}" }
        return byShowWeek.any { (_, es) ->
            val days = es.map { it.date.dayOfWeek.value }.toSet()
            days.contains(6) && days.contains(7)
        }
    }
}
