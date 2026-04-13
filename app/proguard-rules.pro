# ─────────────────────────────────────────────────────────────────────────────
# ShowMate — ProGuard / R8 Rules
# ─────────────────────────────────────────────────────────────────────────────
# Notas:
#  · -dontoptimize: desactiva optimizaciones agresivas de R8 (evita crashes
#    con SQLCipher y reflexión de Hilt en algunos dispositivos).
#  · El shrinking (eliminación de código muerto) sigue activo.
#  · No usar -keep class com.andrea.showmateapp.** { *; } globalmente:
#    impide todo shrinking y es la mayor fuente de APKs grandes.
# ─────────────────────────────────────────────────────────────────────────────

-dontoptimize

# ── Atributos (una sola vez) ──────────────────────────────────────────────────
-keepattributes Signature, InnerClasses, EnclosingMethod,
                RuntimeVisibleAnnotations, AnnotationDefault, *Annotation*

# ── Kotlin ────────────────────────────────────────────────────────────────────
-keep class kotlin.Metadata { *; }
-dontwarn kotlin.**
-dontwarn kotlinx.**

# ── Kotlin Serialization (Navigation type-safe con @Serializable) ─────────────
# SIN ESTO las rutas crashean en release con ClassNotFoundException
-keep @kotlinx.serialization.Serializable class * { *; }
-keepclassmembers @kotlinx.serialization.Serializable class * {
    *** Companion;
    kotlinx.serialization.KSerializer serializer(...);
    <fields>;
}
-keepclasseswithmembers class * {
    @kotlinx.serialization.SerialName <fields>;
}
-dontnote kotlinx.serialization.AnnotationsKt

# ── Coroutines ────────────────────────────────────────────────────────────────
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}

# ── Hilt / Dagger ─────────────────────────────────────────────────────────────
-dontwarn dagger.hilt.**
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keep @dagger.hilt.android.HiltAndroidApp class * { *; }
-keep @dagger.hilt.InstallIn class * { *; }
-keep @dagger.Module class * { *; }
-keep class * extends androidx.lifecycle.ViewModel { *; }
-keepclassmembers class * extends androidx.lifecycle.ViewModel {
    <init>(...);
}

# ── Modelos de datos (Firestore, Retrofit, Room) ──────────────────────────────
-keep class com.andrea.showmateapp.data.model.** { *; }
-keep class com.andrea.showmateapp.data.network.** { *; }
-keep class com.andrea.showmateapp.data.local.** { *; }
-keep class com.andrea.showmateapp.domain.model.** { *; }
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# ── DI modules y domain (Hilt necesita los constructores) ─────────────────────
-keep class com.andrea.showmateapp.di.** { *; }
-keep class com.andrea.showmateapp.domain.** { *; }

# ── Utilidades críticas ───────────────────────────────────────────────────────
-keep class com.andrea.showmateapp.util.DatabaseKeyProvider { *; }
-keep class com.andrea.showmateapp.util.SecurityChecker { *; }

# ── Retrofit / OkHttp ─────────────────────────────────────────────────────────
-keep class retrofit2.** { *; }
-keep class okhttp3.** { *; }
-keep class com.google.gson.** { *; }
-dontwarn retrofit2.**
-dontwarn okhttp3.**
-dontwarn okio.**

# ── Firebase ──────────────────────────────────────────────────────────────────
-keep class com.google.firebase.** { *; }
-keep class com.google.android.gms.** { *; }
-dontwarn com.google.firebase.**
-dontwarn com.google.android.gms.**

# ── Room ──────────────────────────────────────────────────────────────────────
-keep class * extends androidx.room.RoomDatabase { *; }
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao interface * { *; }
-keep @androidx.room.Database class * { *; }

# ── SQLCipher ─────────────────────────────────────────────────────────────────
-keep class net.sqlcipher.** { *; }
-keep class net.sqlcipher.database.** { *; }

# ── WorkManager (SIN ESTO los workers no arrancan en release) ─────────────────
-keep class * extends androidx.work.Worker { *; }
-keep class * extends androidx.work.ListenableWorker { *; }
-keepclassmembers class * extends androidx.work.ListenableWorker {
    public <init>(android.content.Context, androidx.work.WorkerParameters);
}

# ── Coil ──────────────────────────────────────────────────────────────────────
-keep class coil.** { *; }
-keep interface coil.** { *; }
-dontwarn coil.**

# ── Paging 3 ──────────────────────────────────────────────────────────────────
-keep class androidx.paging.** { *; }
-dontwarn androidx.paging.**

# ── Navigation Compose ────────────────────────────────────────────────────────
-keep class androidx.navigation.** { *; }
-dontwarn androidx.navigation.**

# ── DataStore ─────────────────────────────────────────────────────────────────
-keep class androidx.datastore.** { *; }
