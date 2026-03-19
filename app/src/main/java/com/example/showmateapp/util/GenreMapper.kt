package com.example.showmateapp.util

object GenreMapper {
    fun getGenreName(id: String): String = when (id) {
        "10759" -> "Acción y Aventura"
        "16"    -> "Animación"
        "35"    -> "Comedia"
        "80"    -> "Crimen"
        "99"    -> "Documental"
        "18"    -> "Drama"
        "10751" -> "Familiar"
        "10762" -> "Infantil"
        "9648"  -> "Misterio"
        "10763" -> "Noticias"
        "10764" -> "Reality"
        "10765" -> "Sci-Fi y Fantasía"
        "10766" -> "Soap"
        "10767" -> "Talk"
        "10768" -> "Guerra y Política"
        "37"    -> "Western"
        else    -> "Series Variadas"
    }
}
