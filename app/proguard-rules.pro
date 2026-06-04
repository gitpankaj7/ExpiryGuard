# Add project specific ProGuard rules here.

# ── Room Database ──
-keep class com.expiryguard.app.data.local.** { *; }
-keepclassmembers class com.expiryguard.app.data.local.ProductEntity {
    <init>(...);
}

# ── ML Kit / Google Play Services ──
-keep class com.google.mlkit.** { *; }
-keep class com.google.android.gms.vision.** { *; }
-keep class com.google.android.gms.internal.mlkit_vision_** { *; }
-keep class com.google.android.gms.** { *; }
-dontwarn com.google.mlkit.**
-dontwarn com.google.android.gms.**

# ── Network / JSON (for barcode API lookup) ──
-keep class org.json.** { *; }
-keep class java.net.** { *; }
-dontwarn java.net.**
-dontwarn org.json.**

# ── WorkManager ──
-keep class com.expiryguard.app.worker.** { *; }
-keep class androidx.work.** { *; }

# ── Kotlin Coroutines ──
-dontwarn kotlinx.coroutines.**
-keep class kotlinx.coroutines.** { *; }

# ── DataStore ──
-keep class androidx.datastore.** { *; }
-dontwarn androidx.datastore.**

# ── Compose ──
-dontwarn androidx.compose.**

# ── CameraX ──
-keep class androidx.camera.** { *; }
-dontwarn androidx.camera.**

# ── App classes ──
-keep class com.expiryguard.app.util.** { *; }
-keep class com.expiryguard.app.di.** { *; }
-keep class com.expiryguard.app.ExpiryGuardApp { *; }

# ── General ──
-keepattributes *Annotation*
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# ── Keep Kotlin metadata ──
-keep class kotlin.Metadata { *; }
-dontwarn kotlin.**
