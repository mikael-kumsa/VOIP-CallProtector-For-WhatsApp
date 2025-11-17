# WhatsApp Call Protector - ProGuard Rules
# Production-ready configuration for release builds

# Keep line numbers for crash reporting
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Keep all services and important classes
-keep class com.lal.voipcallprotector.service.** { *; }
-keep class com.lal.voipcallprotector.accessibility.** { *; }
-keep class com.lal.voipcallprotector.util.** { *; }
-keep class com.lal.voipcallprotector.ui.** { *; }

# Keep all Activities
-keep class com.lal.voipcallprotector.*Activity { *; }

# Keep BuildConfig
-keep class com.lal.voipcallprotector.BuildConfig { *; }

# Keep Android Lifecycle components
-keep class androidx.lifecycle.** { *; }
-keepclassmembers class * extends androidx.lifecycle.ViewModel {
    <init>(...);
}

# Keep Coroutines
-keepclassmembernames class kotlinx.** {
    volatile <fields>;
}
-keep class kotlinx.coroutines.** { *; }
-dontwarn kotlinx.coroutines.**

# Keep Parcelable implementations
-keep class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator *;
}

# Keep Serializable classes
-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}

# Keep native methods
-keepclasseswithmembernames class * {
    native <methods>;
}

# Keep enums
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Keep R class
-keep class com.lal.voipcallprotector.R$* { *; }

# Keep data classes
-keepclassmembers class * {
    @kotlin.jvm.JvmField <fields>;
}

# Remove logging in release (optional - uncomment if you want to remove all logs)
# -assumenosideeffects class android.util.Log {
#     public static boolean isLoggable(java.lang.String, int);
#     public static int v(...);
#     public static int i(...);
#     public static int w(...);
#     public static int d(...);
#     public static int e(...);
# }

# Google Play Services
-keep class com.google.android.gms.** { *; }
-dontwarn com.google.android.gms.**

# Material Components
-keep class com.google.android.material.** { *; }
-dontwarn com.google.android.material.**