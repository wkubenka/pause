# Type-safe navigation routes are @Serializable classes; keep them and their serializers.
-keep @kotlinx.serialization.Serializable class com.astute.pause.** { *; }
-keepclassmembers class com.astute.pause.** {
    *** Companion;
    kotlinx.serialization.KSerializer serializer(...);
}
