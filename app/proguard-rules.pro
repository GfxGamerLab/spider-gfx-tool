# R8 / ProGuard Rules for Spider GFX Tool v9.0

# Keep App Entry Points & Activities
-keep public class com.example.spidergfxtool.MainActivity { *; }
-keep class com.example.spidergfxtool.** { *; }

# Shizuku API & Provider
-keep class dev.rikka.shizuku.** { *; }
-keep interface dev.rikka.shizuku.** { *; }
-keep class moe.shizuku.privileged.api.** { *; }

# OkHttp & Okio
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }

# Google AdMob SDK
-keep class com.google.android.gms.ads.** { *; }
-dontwarn com.google.android.gms.ads.**

# Coil Image Loading
-dontwarn coil.**
-keep class coil.** { *; }

# Jetpack Compose & Material
-keepattributes *Annotation*, Signature, InnerClasses, EnclosingMethod
-keep class androidx.compose.** { *; }

# Parcelable and Serializable models
-keepclassmembers class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator CREATOR;
}
-keepclassmembers class * implements java.io.Serializable {
    private static final long serialVersionUID;
    readResolve();
    writeReplace();
    readObject(java.io.ObjectInputStream);
    writeObject(java.io.ObjectOutputStream);
}

# Strip verbose Log calls in release builds to reduce APK footprint and improve performance
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int d(...);
    public static int i(...);
}
