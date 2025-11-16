# ========== DJI SDK ProGuard Rules ==========

# DJI SDK
-keep class dji.v5.** { *; }
-keep class dji.sdk.** { *; }
-keep class com.dji.** { *; }

# Keep all native methods
-keepclasseswithmembernames class * {
    native <methods>;
}

# Keep serialization related
-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}

# Keep Parcelable
-keep class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator *;
}

# Keep annotations and metadata
-keepattributes *Annotation*, Signature, InnerClasses, EnclosingMethod

# Keep application classes
-keep class com.brainai.skypilot.** { *; }

# Disable optimization for DJI SDK
-dontwarn dji.**
-dontwarn com.dji.**

# For debug builds, disable all optimization
-dontobfuscate
-dontoptimize
-dontpreverify