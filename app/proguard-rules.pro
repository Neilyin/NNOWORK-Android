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

# Play review-ktx 參照的 gms 編譯期註解（執行時不需要）。
-dontwarn com.google.android.gms.common.annotation.NoNullnessRewrite

# Moshi 用 KotlinJsonAdapterFactory（反射）：data 層有不少沒標 @JsonClass 的模型（例如 JobArchiveRemoteRow、RewardEvent），
# 被 R8 改名會讓 JSON 欄位對不上。整個 data 層保留類別與欄位名稱。
-keep class com.neilyin.nnowork.data.** { *; }
