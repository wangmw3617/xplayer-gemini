# Room Database
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**

# Media3 & ExoPlayer
-keep class androidx.media3.** { *; }
-dontwarn androidx.media3.**

# NextLib Media3 Extensions
-keep class io.github.anilbeesetti.nextlib.** { *; }
-dontwarn io.github.anilbeesetti.nextlib.**

# libmpv / MPVLib JNI
-keep class is.xyz.mpv.** { *; }
-keepclassmembers class is.xyz.mpv.** {
    native <methods>;
}
-dontwarn is.xyz.mpv.**

# Data Models & Room Entities
-keep class com.xplayer.app.data.local.entity.** { *; }
-keep class com.xplayer.app.data.datastore.** { *; }
-keep class com.xplayer.app.player.model.** { *; }

# Coil
-dontwarn coil.**
