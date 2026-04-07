# ── Debug info ────────────────────────────────────────────────────────────────
# Keep source file names + line numbers for crash stack traces (Crashlytics)
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# ── Kotlin ────────────────────────────────────────────────────────────────────
-keepattributes Signature, InnerClasses, EnclosingMethod
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations
-keepattributes *Annotation*
-dontwarn kotlin.Unit
-dontwarn kotlin.**
-keepclassmembernames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepclassmembernames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembernames class kotlinx.** { volatile <fields>; }

# ── Retrofit ──────────────────────────────────────────────────────────────────
-keepclassmembers,allowshrinking,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}
-dontwarn org.codehaus.mojo.animal_sniffer.IgnoreJRERequirement
-dontwarn javax.annotation.**
-dontwarn retrofit2.KotlinExtensions
-dontwarn retrofit2.KotlinExtensions$*

# ── OkHttp ────────────────────────────────────────────────────────────────────
-dontwarn okhttp3.**
-dontwarn okio.**
-keepnames class okhttp3.internal.publicsuffix.PublicSuffixDatabase

# ── Gson / SerializedName ─────────────────────────────────────────────────────
-dontwarn sun.misc.**
-keep class com.google.gson.stream.** { *; }
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer
-keepclassmembers,allowobfuscation class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# Keep TMDB network models and Room entities (used by Gson + Firestore toObject())
-keep class com.andrea.showmateapp.data.network.** { *; }
-keep class com.andrea.showmateapp.data.model.** { *; }

# ── kotlinx.serialization ─────────────────────────────────────────────────────
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** { *** Companion; }
-keepclasseswithmembers class **$$serializer {
    kotlinx.serialization.descriptors.SerialDescriptor descriptor;
}
-keepclassmembers @kotlinx.serialization.Serializable class ** {
    *** Companion;
    *** serialVersionUID;
    kotlinx.serialization.KSerializer serializer(...);
}

# ── Firebase ──────────────────────────────────────────────────────────────────
# Keep names only (class name obfuscation breaks Firebase reflection internals)
-keepnames class com.google.firebase.** { *; }
-keepnames class com.google.android.gms.** { *; }
-dontwarn com.google.firebase.**
-dontwarn com.google.android.gms.**
# Firestore: keep no-arg constructors for toObject() deserialization
-keepclassmembers class com.andrea.showmateapp.data.model.** {
    public <init>();
}
-keepclassmembers class com.andrea.showmateapp.data.network.** {
    public <init>();
}

# ── Room ──────────────────────────────────────────────────────────────────────
-keep class * extends androidx.room.RoomDatabase { *; }
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao interface * { *; }
-keepclassmembers @androidx.room.Entity class * { *; }
-dontwarn androidx.room.**

# ── SQLCipher ─────────────────────────────────────────────────────────────────
-keep class net.sqlcipher.** { *; }
-keep class net.sqlcipher.database.** { *; }
-dontwarn net.sqlcipher.**

# ── Hilt / Dagger ─────────────────────────────────────────────────────────────
-dontwarn dagger.hilt.**
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keep @dagger.hilt.android.HiltAndroidApp class * { *; }
-keep @dagger.hilt.InstallIn class * { *; }
-keep @dagger.Module class * { *; }
# HiltViewModel — keep all classes annotated with @HiltViewModel
-keep,allowobfuscation @dagger.hilt.android.lifecycle.HiltViewModel class * { *; }
-keep @dagger.hilt.android.lifecycle.HiltViewModel class * { *; }

# ── Jetpack Compose ───────────────────────────────────────────────────────────
# R8 + Compose work without extra rules since Compose 1.3; keep lambda stability
-keepclassmembers class * {
    @androidx.compose.runtime.Stable <methods>;
}

# ── Navigation Compose (type-safe routes via @Serializable) ──────────────────
-keep @kotlinx.serialization.Serializable class * { *; }

# ── WorkManager + Hilt Workers ────────────────────────────────────────────────
-keep class * extends androidx.work.Worker { *; }
-keep class * extends androidx.work.CoroutineWorker { *; }
-keep class * extends androidx.work.ListenableWorker {
    public <init>(android.content.Context, androidx.work.WorkerParameters);
}
-keep @androidx.hilt.work.HiltWorker class * { *; }

# ── DataStore ─────────────────────────────────────────────────────────────────
-dontwarn androidx.datastore.**

# ── Coil ──────────────────────────────────────────────────────────────────────
-dontwarn coil.**

# ── Timber (strip all log levels from release) ───────────────────────────────
-assumenosideeffects class timber.log.Timber {
    public static *** v(...);
    public static *** d(...);
    public static *** i(...);
    public static *** w(...);
}

# ── Security Crypto (EncryptedSharedPreferences / MasterKey) ─────────────────
-keep class androidx.security.crypto.** { *; }
-dontwarn androidx.security.crypto.**

# ── General ───────────────────────────────────────────────────────────────────
# Keep @Keep annotated classes/members (emergency escape hatch)
-keep @androidx.annotation.Keep class * { *; }
-keepclassmembers class * {
    @androidx.annotation.Keep *;
}
