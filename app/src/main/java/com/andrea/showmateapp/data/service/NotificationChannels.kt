package com.andrea.showmateapp.data.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context

object NotificationChannels {

    const val SEASON = "new_season"
    const val FRIEND_ACTIVITY = "friend_activity"
    const val INACTIVITY_RECS = "inactivity_recs"
    const val HIDDEN_GEM = "hidden_gem"
    const val STREAK_DANGER = "streak_danger"
    const val GENERAL = "showmate_general"

    fun createAll(context: Context) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val channels = listOf(
            NotificationChannel(
                SEASON,
                "Nuevas temporadas",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Avisa cuando una serie de tu watchlist o favoritos estrena temporada"
                enableLights(true)
                enableVibration(true)
            },
            NotificationChannel(
                FRIEND_ACTIVITY,
                "Actividad de amigos",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Cuando un amigo con alta compatibilidad termina una serie"
            },
            NotificationChannel(
                INACTIVITY_RECS,
                "Recomendaciones del día",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Sugerencias personalizadas cuando llevas varios días sin ver nada"
            },
            NotificationChannel(
                HIDDEN_GEM,
                "Joya oculta del día",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Una serie con alta afinidad para ti pero poco conocida, una vez al día"
            },
            NotificationChannel(
                STREAK_DANGER,
                "Racha en peligro",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Alerta urgente cuando tu racha de varios días está a punto de romperse"
                enableVibration(true)
            },
            NotificationChannel(
                GENERAL,
                "Notificaciones generales",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notificaciones push genéricas de ShowMate"
            }
        )

        channels.forEach { nm.createNotificationChannel(it) }
    }
}
