# R8 / ProGuard Rules for Spider GFX Tool v9.0

# -------------------------------------------------------------------------
# Keep App Entry Points, ViewModel, Services, Receivers & Internal Package
# -------------------------------------------------------------------------
-keep class com.example.spidergfxtool.** { *; }
-keepclassmembers class com.example.spidergfxtool.** { *; }
-keepclassmembers class * extends androidx.lifecycle.ViewModel { <init>(...); }

# Keep App Components
-keep public class * extends android.app.Activity
-keep public class * extends android.app.Application
-keep public class * extends android.app.Service
-keep public class * extends android.content.BroadcastReceiver
-keep public class * extends android.content.ContentProvider

# -------------------------------------------------------------------------
# Shizuku API & Rikka Libraries (CRITICAL: preserve all packages)
# -------------------------------------------------------------------------
-keep class rikka.shizuku.** { *; }
-keep interface rikka.shizuku.** { *; }
-keep class dev.rikka.shizuku.** { *; }
-keep interface dev.rikka.shizuku.** { *; }
-keep class moe.shizuku.** { *; }
-keep interface moe.shizuku.** { *; }
-keep class rikka.** { *; }
-dontwarn rikka.shizuku.**
-dontwarn dev.rikka.shizuku.**
-dontwarn moe.shizuku.**

# -------------------------------------------------------------------------
# OkHttp, Okio, & Network Clients
# -------------------------------------------------------------------------
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }

# -------------------------------------------------------------------------
# Google AdMob SDK
# -------------------------------------------------------------------------
-keep class com.google.android.gms.ads.** { *; }
-dontwarn com.google.android.gms.ads.**

# -------------------------------------------------------------------------
# Coil Image Loading
# -------------------------------------------------------------------------
-dontwarn coil.**
-keep class coil.** { *; }

# -------------------------------------------------------------------------
# Jetpack Compose & Material 3
# -------------------------------------------------------------------------
-keepattributes *Annotation*, Signature, InnerClasses, EnclosingMethod
-keep class androidx.compose.** { *; }
-keep class androidx.compose.material3.** { *; }
-keep class androidx.compose.runtime.** { *; }

# -------------------------------------------------------------------------
# Parcelable and Serializable models
# -------------------------------------------------------------------------
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

# Preserve Log class safely to avoid optimization side-effect issues
-dontwarn android.util.Log
