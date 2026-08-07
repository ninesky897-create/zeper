# Zeper Player ProGuard Rules

# Retrofit & OkHttp
-keepattributes Signature, InnerClasses, AnnotationDefault
-dontwarn retrofit2.**
-keep class retrofit2.** { *; }
-keepattributes RuntimeVisibleAnnotations, RuntimeInvisibleAnnotations
-dontwarn okio.**
-dontwarn javax.annotation.**
-dontwarn org.codehaus.mojo.animal_sniffer.**

# Room
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**

# Coil
-keep class coil.compose.AsyncImageKt { *; }
-dontwarn coil.**

# YoutubeDL-Android (Crucial for JNI)
-keep class com.yausername.youtubedl_android.** { *; }
-keep class com.yausername.ffmpeg.** { *; }
-keep class com.yausername.aria2c.** { *; }
-keep class io.github.junkfood02.youtubedl_android.** { *; }

# JNI
-keepclasseswithmembernames class * {
    native <methods>;
}

# Compose
# -keepclassmembers class androidx.compose.runtime.Recomposer {
#    private void readObject(java.io.ObjectInputStream);
# }

# Android XR / Glimmer (if any)
-dontwarn com.google.android.glimmer.**

# General
-keepattributes SourceFile,LineNumberTable
-keep class com.zeper.player.core.data.** { *; }