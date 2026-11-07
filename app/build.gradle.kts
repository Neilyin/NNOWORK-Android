import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

// 只有在放入 google-services.json 後才套用 Firebase 外掛，
// 沒有設定檔時 App 仍可正常編譯（FCM/Analytics 會自動停用）。
if (file("google-services.json").exists()) {
    apply(plugin = "com.google.gms.google-services")
}

// 讀取簽章設定（keystore.properties 不進版控）。
val keystorePropsFile = rootProject.file("keystore.properties")
val keystoreProps = Properties().apply {
    if (keystorePropsFile.exists()) keystorePropsFile.inputStream().use { load(it) }
}

android {
    namespace = "com.neilyin.nnowork"
    // Android 16 QPR SDK is installed on the release workstation.
    // AGP 8.13+ models its 36.1 minor API explicitly.
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    signingConfigs {
        if (keystorePropsFile.exists()) {
            create("release") {
                storeFile = rootProject.file(keystoreProps.getProperty("storeFile"))
                storePassword = keystoreProps.getProperty("storePassword")
                keyAlias = keystoreProps.getProperty("keyAlias")
                keyPassword = keystoreProps.getProperty("keyPassword")
            }
        }
    }

    defaultConfig {
        // Play Console 上既有 App 的套件名稱（必須一致才能上傳）。
        // 注意：這是對外的 applicationId；程式內部 namespace 仍維持 com.neilyin.nnowork。
        applicationId = "com.chi157.resignationpointscard"
        minSdk = 26
        targetSdk = 36
        versionCode = 1279
        versionName = "3.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables { useSupportLibrary = true }
    }

    buildTypes {
        release {
            // Play 上架包啟用 R8 縮減／最佳化／混淆，並移除無用資源。
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            // 有 keystore.properties 才套用 release 簽章。
            if (keystorePropsFile.exists()) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    // Room 匯出的 schema JSON 供 MigrationTestHelper（androidTest）與 JVM 遷移測試讀取。
    sourceSets {
        getByName("androidTest").assets.srcDir("$projectDir/schemas")
    }
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    // App 回前景（ProcessLifecycleOwner）時補拉過往工作（§19.3）。
    implementation("androidx.lifecycle:lifecycle-process:${libs.versions.lifecycle.get()}")
    implementation(libs.androidx.activity.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.navigation.compose)
    // 摺疊機摺痕位置（FoldingFeature），供 ui/adaptive/FoldInfo.kt 依 hinge 分欄
    implementation(libs.androidx.window)
    debugImplementation(libs.androidx.ui.tooling)

    // Room
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    // DataStore
    implementation(libs.androidx.datastore.preferences)

    // Networking (Supabase REST/RPC)
    implementation(libs.retrofit)
    implementation(libs.retrofit.moshi)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)
    implementation(libs.moshi)
    implementation(libs.moshi.kotlin)

    implementation(libs.kotlinx.coroutines.android)

    // Widget (Glance) + background refresh
    implementation(libs.androidx.glance.appwidget)
    implementation(libs.androidx.glance.material3)
    implementation(libs.androidx.work.runtime.ktx)

    // Image loading (avatars / icons in tree hole)
    implementation(libs.coil.compose)

    // Google Play In-App Updates / In-App Review（Phase 5b）
    implementation(libs.play.app.update.ktx)
    implementation(libs.play.review.ktx)

    // Firebase（FCM 推播 + Analytics）。需放入 google-services.json 才會實際運作。
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.messaging)
    implementation(libs.firebase.analytics)

    testImplementation("junit:junit:4.13.2")
    // JVM 端以真的 SQLite 執行 v4 → v5 遷移 SQL（不需模擬器）。
    testImplementation("org.xerial:sqlite-jdbc:3.41.2.2")

    // Room 遷移的 instrumented test（MigrationTestHelper）。
    androidTestImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test:runner:1.6.2")
    androidTestImplementation("androidx.room:room-testing:${libs.versions.room.get()}")
}
