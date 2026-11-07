# Moshi 的 KotlinJsonAdapterFactory 使用反射讀寫 DTO。
# 只保留明確標記為 JSON model 的類別；其餘 UI／商業邏輯仍可由 R8 縮減與混淆。
-keep,allowoptimization @com.squareup.moshi.JsonClass class * { *; }

# 加密 session 也以 Moshi 反射序列化，但刻意不對外暴露 JSON model 註解。
-keep,allowoptimization class com.neilyin.nnowork.data.auth.StoredAuthSession { *; }

# 保留 Retrofit/Moshi 解析所需的 generic 與 annotation metadata。
-keepattributes Signature
-keepattributes RuntimeVisibleAnnotations,RuntimeInvisibleAnnotations
-keepattributes AnnotationDefault
-keep class kotlin.Metadata { *; }
