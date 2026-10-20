plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.example.floatingclock"
    compileSdk {
        version = release(36)
    }

    defaultConfig {
        applicationId = "com.example.floatingclock"
        minSdk = 21
        targetSdk = 36
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
    kotlinOptions {
        jvmTarget = "11"
    }
    androidResources {
        localeFilters += listOf("en", "es", "pt", "fr")
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.tv.foundation)
    implementation(libs.androidx.tv.material)
    implementation(libs.androidx.lifecycle.runtime)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.service)
    implementation(libs.androidx.activity.compose)
    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.test.runner)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}

val translationVerifier = rootProject.file("tools/TranslationVerifier.java")
val verifyTranslations by tasks.registering(Exec::class) {
    group = "verification"
    description = "Check complete translation catalogs and compatible placeholders."
    inputs.file(translationVerifier)
    inputs.dir("src/main/res")
    commandLine("${System.getProperty("java.home")}/bin/java", translationVerifier, file("src/main/res"))
}
val testTranslationVerifier by tasks.registering(Exec::class) {
    group = "verification"
    commandLine("${System.getProperty("java.home")}/bin/java", translationVerifier, "--self-test")
}
tasks.named("check") { dependsOn(verifyTranslations, testTranslationVerifier) }
