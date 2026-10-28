import java.security.MessageDigest
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

val localProperties = Properties().apply {
    rootProject.file("local.properties").takeIf { it.isFile }?.inputStream()?.use(::load)
}
val reviewAccessCode = providers.environmentVariable("FLOATING_CLOCK_REVIEW_CODE").orNull
    ?: localProperties.getProperty("floatingClockReviewCode")
val normalizedReviewAccessCode = reviewAccessCode.orEmpty()
    .filter(Char::isLetterOrDigit)
    .uppercase()
require(
    normalizedReviewAccessCode.isEmpty() ||
        normalizedReviewAccessCode.matches(Regex("[0-9A-F]{32}"))
) {
    "floatingClockReviewCode must be exactly 32 hexadecimal characters (128 bits)."
}
val reviewAccessCodeSha256 = if (normalizedReviewAccessCode.isEmpty()) {
    ""
} else {
    MessageDigest.getInstance("SHA-256")
        .digest(normalizedReviewAccessCode.toByteArray(Charsets.UTF_8))
        .joinToString("") { it.toInt().and(0xff).toString(16).padStart(2, '0') }
}
val uploadStoreFile = localProperties.getProperty("floatingClockUploadStoreFile")
val uploadStorePassword = localProperties.getProperty("floatingClockUploadStorePassword")
val uploadKeyAlias = localProperties.getProperty("floatingClockUploadKeyAlias")
val uploadKeyPassword = localProperties.getProperty("floatingClockUploadKeyPassword")
val hasAnyUploadSigningValue = listOf(
    uploadStoreFile,
    uploadStorePassword,
    uploadKeyAlias,
    uploadKeyPassword
).any { !it.isNullOrBlank() }
val hasCompleteUploadSigningConfig = listOf(
    uploadStoreFile,
    uploadStorePassword,
    uploadKeyAlias,
    uploadKeyPassword
).all { !it.isNullOrBlank() }
require(!hasAnyUploadSigningValue || hasCompleteUploadSigningConfig) {
    "Set all four floatingClockUpload* values in the ignored local.properties file."
}

android {
    namespace = "com.marianobattaglia.floatingclock"
    compileSdk {
        version = release(36)
    }

    defaultConfig {
        applicationId = "com.marianobattaglia.floatingclock"
        minSdk = 21
        targetSdk = 36
        versionCode = 2
        versionName = "1.0.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        buildConfigField("String", "REVIEW_ACCESS_CODE_SHA256", "\"\"")

    }

    if (hasCompleteUploadSigningConfig) {
        signingConfigs {
            create("upload") {
                storeFile = file(uploadStoreFile!!)
                storePassword = uploadStorePassword
                keyAlias = uploadKeyAlias
                keyPassword = uploadKeyPassword
            }
        }
    }

    buildTypes {
        release {
            buildConfigField(
                "String",
                "REVIEW_ACCESS_CODE_SHA256",
                "\"$reviewAccessCodeSha256\""
            )
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = if (hasCompleteUploadSigningConfig) {
                signingConfigs.getByName("upload")
            } else {
                null
            }
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
        buildConfig = true
    }
}

tasks.configureEach {
    if (name == "bundleRelease" || name == "assembleRelease") {
        doFirst {
            check(hasCompleteUploadSigningConfig) {
                "Configure the upload keystore in the ignored local.properties file before building release."
            }
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.tv.foundation)
    implementation(libs.androidx.tv.material)
    implementation(libs.androidx.lifecycle.runtime)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.service)
    implementation(libs.androidx.activity.compose)
    implementation(libs.google.play.billing)
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
