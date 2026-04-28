package com.andrea.showmateapp.util

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.andrea.showmateapp.data.model.UserProfile
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import org.json.JSONException
import org.json.JSONObject
import timber.log.Timber

@Singleton
class DataExportManager @Inject constructor(
    @ApplicationContext private val context: Context
) {

    fun buildJsonExport(profile: UserProfile): String {
        val sb = StringBuilder()
        sb.appendLine("{")
        sb.appendLine("  \"username\": \"${profile.username}\",")
        sb.appendLine("  \"email\": \"${profile.email}\",")
        sb.appendLine("  \"xp\": ${profile.xp},")
        sb.appendLine("  \"likedMedia\": ${profile.likedMediaIds},")
        sb.appendLine("  \"essentialMedia\": ${profile.essentialMediaIds},")
        sb.appendLine("  \"dislikedMedia\": ${profile.dislikedMediaIds},")
        sb.appendLine(
            "  \"ratings\": ${profile.ratings.entries.joinToString(",", "{", "}") { "\"${it.key}\": ${it.value}" }},"
        )
        sb.appendLine(
            "  \"customLists\": ${profile.customLists.entries.joinToString(
                ",",
                "{",
                "}"
            ) { "\"${it.key}\": ${it.value}" }},"
        )
        sb.appendLine(
            "  \"genreScores\": ${profile.genreScores.entries.joinToString(
                ",",
                "{",
                "}"
            ) { "\"${it.key}\": ${it.value}" }},"
        )
        sb.appendLine(
            "  \"watchedEpisodes\": ${profile.watchedEpisodes.entries.joinToString(
                ",",
                "{",
                "}"
            ) { "\"${it.key}\": ${it.value}" }},"
        )
        sb.appendLine("  \"achievements\": ${profile.unlockedAchievementIds},")
        sb.appendLine(
            "  \"exportedAt\": \"${SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()).format(Date())}\""
        )
        sb.appendLine("}")
        return sb.toString()
    }

    fun buildCsvExport(profile: UserProfile): String {
        val sb = StringBuilder()
        sb.appendLine("tipo,id,valor")
        profile.likedMediaIds.forEach { sb.appendLine("liked,$it,") }
        profile.essentialMediaIds.forEach { sb.appendLine("essential,$it,") }
        profile.dislikedMediaIds.forEach { sb.appendLine("disliked,$it,") }
        profile.ratings.forEach { (id, rating) -> sb.appendLine("rating,$id,$rating") }
        profile.customLists.forEach { (listName, ids) ->
            ids.forEach { id -> sb.appendLine("list_${listName.replace(",", ";")},$id,") }
        }
        return sb.toString()
    }

    fun readUriContent(uri: Uri): String? {
        return try {
            context.contentResolver.openInputStream(uri)?.use { it.bufferedReader().readText() }
        } catch (e: Exception) {
            Timber.e(e, "Error reading URI content")
            null
        }
    }

    fun parseJsonBackup(content: String): UserProfile? {
        return try {
            val json = JSONObject(content)

            fun jsonArrayToIntList(key: String): List<Int> {
                val arr = json.optJSONArray(key) ?: return emptyList()
                return (0 until arr.length()).map { arr.getInt(it) }
            }

            fun jsonObjToFloatMap(key: String): Map<String, Float> {
                val obj = json.optJSONObject(key) ?: return emptyMap()
                val map = mutableMapOf<String, Float>()
                obj.keys().forEach { k -> map[k] = obj.getDouble(k).toFloat() }
                return map
            }

            fun jsonObjToIntListMap(key: String): Map<String, List<Int>> {
                val obj = json.optJSONObject(key) ?: return emptyMap()
                val map = mutableMapOf<String, List<Int>>()
                obj.keys().forEach { k ->
                    val arr = obj.optJSONArray(k) ?: return@forEach
                    map[k] = (0 until arr.length()).map { arr.getInt(it) }
                }
                return map
            }

            UserProfile(
                username = json.optString("username", ""),
                likedMediaIds = jsonArrayToIntList("likedMedia"),
                essentialMediaIds = jsonArrayToIntList("essentialMedia"),
                dislikedMediaIds = jsonArrayToIntList("dislikedMedia"),
                ratings = jsonObjToFloatMap("ratings"),
                customLists = jsonObjToIntListMap("customLists"),
                genreScores = jsonObjToFloatMap("genreScores"),
                watchedEpisodes = jsonObjToIntListMap("watchedEpisodes"),
                xp = json.optInt("xp", 0)
            )
        } catch (e: JSONException) {
            Timber.e(e, "Invalid backup JSON")
            null
        }
    }

    fun saveToDownloads(content: String, filename: String, mimeType: String = "application/json"): Uri? {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val resolver = context.contentResolver
                val contentValues = ContentValues().apply {
                    put(MediaStore.Downloads.DISPLAY_NAME, filename)
                    put(MediaStore.Downloads.MIME_TYPE, mimeType)
                    put(MediaStore.Downloads.IS_PENDING, 1)
                }
                val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                    ?: return null
                resolver.openOutputStream(uri)?.use { it.write(content.toByteArray()) }
                contentValues.clear()
                contentValues.put(MediaStore.Downloads.IS_PENDING, 0)
                resolver.update(uri, contentValues, null, null)
                uri
            } else {
                val dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                val file = File(dir, filename)
                FileOutputStream(file).use { it.write(content.toByteArray()) }
                Uri.fromFile(file)
            }
        } catch (e: Exception) {
            Timber.e(e, "Error saving export file")
            null
        }
    }
}
