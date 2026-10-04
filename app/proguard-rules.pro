# Proguard / R8 rules for Sport Amigo (ch.simibu.sport)

# Keep line numbers and file names for readable crash stack traces in Google Play Console
-keepattributes SourceFile,LineNumberTable
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod

# --- Kotlinx Serialization ---
-dontnote kotlinx.serialization.SerializationKt
-keepclassmembers class * {
    @kotlinx.serialization.Serializable <fields>;
}
-keepclassmembers class * {
    @kotlinx.serialization.SerialName <fields>;
}
-keepclassmembers class **$companion {
    public synthetic <fields>;
    public <methods>;
}
-keepclasseswithmembers class * {
    kotlinx.serialization.KSerializer serializer(...);
}

# --- Core Data Models (ch.simibu.sport.core.model) ---
-keep class ch.simibu.sport.core.model.** { *; }
-keepclassmembers class ch.simibu.sport.core.model.** { *; }

# --- Firebase & Google Play Services ---
-keepattributes *Annotation*
-dontwarn com.google.firebase.**
-dontwarn com.google.android.gms.**
-keep class com.google.firebase.firestore.** { *; }

# --- Dagger Hilt ---
-dontwarn dagger.hilt.**
-keep class * extends dagger.hilt.internal.UnsafeCasts { *; }

# --- Kotlin Coroutines ---
-dontwarn kotlinx.coroutines.**

# --- Coil Image Loading ---
-dontwarn coil.**
-keep class coil.** { *; }

# --- Compose & Material Icons ---
-dontwarn androidx.compose.**
