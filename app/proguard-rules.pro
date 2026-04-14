
-dontoptimize

-keepattributes Signature, InnerClasses, EnclosingMethod,
                RuntimeVisibleAnnotations, AnnotationDefault, *Annotation*

-keep class kotlin.Metadata { *; }
-dontwarn kotlin.**
-dontwarn kotlinx.**

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

-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}

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

-keep class com.andrea.showmateapp.data.model.** { *; }
-keep class com.andrea.showmateapp.data.network.** { *; }
-keep class com.andrea.showmateapp.data.local.** { *; }
-keep class com.andrea.showmateapp.domain.model.** { *; }
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

-keep class com.andrea.showmateapp.di.** { *; }
-keep class com.andrea.showmateapp.domain.** { *; }

-keep class com.andrea.showmateapp.util.DatabaseKeyProvider { *; }
-keep class com.andrea.showmateapp.util.SecurityChecker { *; }

-keep class retrofit2.** { *; }
-keep class okhttp3.** { *; }
-keep class com.google.gson.** { *; }
-dontwarn retrofit2.**
-dontwarn okhttp3.**
-dontwarn okio.**

-keep class com.google.firebase.** { *; }
-keep class com.google.android.gms.** { *; }
-dontwarn com.google.firebase.**
-dontwarn com.google.android.gms.**

-keep class * extends androidx.room.RoomDatabase { *; }
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao interface * { *; }
-keep @androidx.room.Database class * { *; }

-keep class net.sqlcipher.** { *; }
-keep class net.sqlcipher.database.** { *; }

-keep class * extends androidx.work.Worker { *; }
-keep class * extends androidx.work.ListenableWorker { *; }
-keepclassmembers class * extends androidx.work.ListenableWorker {
    public <init>(android.content.Context, androidx.work.WorkerParameters);
}

-keep class coil.** { *; }
-keep interface coil.** { *; }
-dontwarn coil.**

-keep class androidx.paging.** { *; }
-dontwarn androidx.paging.**

-keep class androidx.navigation.** { *; }
-dontwarn androidx.navigation.**

-keep class androidx.datastore.** { *; }
