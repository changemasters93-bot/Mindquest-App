# ─── Mindquest ProGuard Rules ────────────────────────────────────────────────
# Keep rules for the composeApp KMP module release build.

# ── Debugging ────────────────────────────────────────────────────────────────
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# ── Kotlin Serialization ─────────────────────────────────────────────────────
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt

-keepclassmembers class kotlinx.serialization.json.** { *** Companion; }
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}

-keep,includedescriptorclasses class com.android.mindquest.**$$serializer { *; }
-keepclassmembers class com.android.mindquest.** {
    *** Companion;
}
-keepclasseswithmembers class com.android.mindquest.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# ── App Domain & DTO Models (used by serialization + reflection) ─────────────
-keep class com.android.mindquest.domain.model.** { *; }
-keep class com.android.mindquest.data.dto.** { *; }

# ── Supabase ─────────────────────────────────────────────────────────────────
-keep class io.github.jan.supabase.** { *; }
-dontwarn io.github.jan.supabase.**

# ── Ktor ─────────────────────────────────────────────────────────────────────
-keep class io.ktor.** { *; }
-dontwarn io.ktor.**
-keep class kotlinx.coroutines.** { *; }
-dontwarn kotlinx.coroutines.**

# ── Koin ─────────────────────────────────────────────────────────────────────
-keep class org.koin.** { *; }
-dontwarn org.koin.**
-keepclassmembers class * {
    public <init>(...);
}

# ── Coil ─────────────────────────────────────────────────────────────────────
-keep class coil3.** { *; }
-dontwarn coil3.**

# ── OkIO / OkHttp (transitive dep) ──────────────────────────────────────────
-dontwarn okhttp3.**
-dontwarn okio.**

# ── Compose (runtime internals) ─────────────────────────────────────────────
-keep class androidx.compose.** { *; }
-dontwarn androidx.compose.**

# ── Firebase ───────────────────────────────────────────────────────────────
-keep class com.google.firebase.** { *; }
-dontwarn com.google.firebase.**
-keep class com.google.android.gms.** { *; }
-dontwarn com.google.android.gms.**

# ── Kotlin Reflect (used by serialization) ───────────────────────────────────
-keep class kotlin.Metadata { *; }
-keep class kotlin.reflect.** { *; }
-dontwarn kotlin.reflect.**
