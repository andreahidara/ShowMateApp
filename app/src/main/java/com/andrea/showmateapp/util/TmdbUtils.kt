package com.andrea.showmateapp.util

object TmdbUtils {
    private const val BASE_URL = "https://image.tmdb.org/t/p/"

    enum class ImageSize(val path: String) {
        W92("w92"),
        W154("w154"),
        W185("w185"),
        W300("w300"),
        W342("w342"),
        W500("w500"),
        W780("w780"),
        W1280("w1280"),
        ORIGINAL("original")
    }

    fun buildImageUrl(path: String?, size: ImageSize = ImageSize.W500): String? {
        if (path == null) return null
        val cleanPath = if (path.startsWith("/")) path else "/$path"
        return "$BASE_URL${size.path}$cleanPath"
    }

    fun getPosterUrl(path: String?, size: ImageSize = ImageSize.W500) = buildImageUrl(path, size)

    fun getBackdropUrl(path: String?, size: ImageSize = ImageSize.W1280) = buildImageUrl(path, size)
}
