package com.andrea.showmateapp.data.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context

object NotificationChannels {

    const val SEASON = "new_season"
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
