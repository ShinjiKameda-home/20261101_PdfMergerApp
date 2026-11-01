import org.gradle.accessors.dm.LibrariesForLibs

// val Any.android: kotlin.Any
// val LibrariesForLibs.pdfbox: Any

plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.example.pdfmerger"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.example.pdfmerger"
        minSdk = 29 // 24 -> 29
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    // PDFBox-Android（バージョンカタログに登録されていない外部ライブラリなので直接記述）
    implementation("com.tom-roush:pdfbox-android:2.0.27.0")
    // implementation(libs.pdfbox.android)

    // 警告を消すため、すべてバージョンカタログ（libs）の参照にする
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity.ktx)
    implementation(libs.androidx.constraintlayout)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}