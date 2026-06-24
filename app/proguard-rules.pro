# Moshi reflection
-keep class com.neilyin.nnowork.data.network.** { *; }
-keepclassmembers class * {
    @com.squareup.moshi.* <fields>;
}
-keep class kotlin.Metadata { *; }
