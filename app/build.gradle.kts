import java.io.FileInputStream
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.kapt)
    alias(libs.plugins.navigation.safeargs)
    alias(libs.plugins.aboutlibraries)
    alias(libs.plugins.google.services)
    alias(libs.plugins.firebase.crashlytics)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "org.phenoapps.intercross"
    compileSdk = libs.versions.compileSdk.get().toInt()

    signingConfigs {
        create("playStoreConfig") {
            val keystorePropsFile = file("keystore.config")
            if (keystorePropsFile.exists()) {
                val keystoreProps = Properties().apply {
                    load(FileInputStream(keystorePropsFile))
                }
                keyAlias = keystoreProps["keyAlias"] as String
                keyPassword = keystoreProps["keyPassword"] as String
                storePassword = keystoreProps["storePassword"] as String
                storeFile = file("intercross.keystore.jks")
            }
        }
    }

    buildFeatures {
        dataBinding = true
        buildConfig = true
        viewBinding = true
        compose = true
    }

    defaultConfig {
        applicationId = "org.phenoapps.intercross"
        minSdk = libs.versions.minSdk.get().toInt()
        targetSdk = libs.versions.targetSdk.get().toInt()
        versionCode = libs.versions.versionCode.get().toInt()
        versionName = libs.versions.versionName.get()
        vectorDrawables.useSupportLibrary = true
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        manifestPlaceholders["appAuthRedirectScheme"] = "fieldbook"

        ksp {
            arg("room.schemaLocation", "$projectDir/schemas")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            isDebuggable = false
            isJniDebuggable = false
            signingConfig = signingConfigs.getByName("playStoreConfig")
            isPseudoLocalesEnabled = false
            isShrinkResources = true
        }

        debug {
            isDebuggable = true
            applicationIdSuffix = ".debug"
            manifestPlaceholders["crashlyticsCollectionEnabled"] = "false"
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    kotlinOptions {
        jvmTarget = JavaVersion.VERSION_21.toString()
    }

    kotlin {
        jvmToolchain(21)
    }

    lint {
        abortOnError = false
        disable += "MissingTranslation"
    }
}

dependencies {
    // Local libs
    implementation(fileTree(mapOf("include" to listOf("*.jar", "*.aar"), "dir" to "libs")))

    // Room
    ksp(libs.androidx.room.compiler)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)

    // Desugar
    coreLibraryDesugaring(libs.desugar.jdk.libs)

    // AndroidX Core
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.activity.ktx)
    implementation(libs.androidx.fragment.ktx)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.recyclerview)
    implementation(libs.androidx.preference.ktx)
    implementation(libs.androidx.collection.ktx)
    implementation(libs.androidx.vectordrawable)
    implementation(libs.androidx.legacy.support.core.utils)
    implementation(libs.androidx.legacy.support.v13)

    // Lifecycle
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.lifecycle.livedata.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.savedstate)
    implementation(libs.androidx.lifecycle.common.java8)

    // Navigation
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)
    implementation(libs.androidx.navigation.runtime.ktx)

    // Compose
    implementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.foundation.layout)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    debugImplementation(libs.androidx.compose.ui.tooling)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material3.windowSizeClass)
    implementation(libs.androidx.compose.material3.adaptive)
    implementation(libs.androidx.hilt.navigation.compose)

    // Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)

    // Firebase
    implementation(libs.firebase.crash)
    implementation(libs.firebase.core)
    implementation(libs.firebase.analytics.ktx)
    implementation(libs.firebase.crashlytics)

    // Google / Material
    implementation(libs.google.material)
    implementation(libs.google.gson)
    implementation(libs.google.guava.listenablefuture)

    // Networking
    implementation(libs.okhttp3)
    implementation(libs.okhttp3.logging.interceptor)
    implementation(libs.okhttp2)
    implementation(libs.gsonfire)

    // Media
    implementation(libs.exoplayer)

    // UVC camera
    implementation(libs.serenegiant.common)

    // BrAPI
    implementation(libs.brapi.java.client)

    // Zebra / Jackson
    implementation(libs.jackson.databind)

    // QR / Barcode
    implementation(libs.zxing.android.embedded)

    // Coroutines
    implementation(libs.kotlin.coroutines.core)
    implementation(libs.kotlin.coroutines.android)

    // Third-party UI
    implementation(libs.mpandroidchart)
    implementation(libs.changelog)
    implementation(libs.aboutlibraries.core)
    implementation(libs.aboutlibraries)
    implementation(libs.tableview)
    implementation(libs.searchpreference)
    // Don't update the next lib
    implementation(libs.material.about.library)

    // Auth
    implementation(libs.appauth)

    // Permissions
    implementation(libs.easypermissions)

    // AppIntro
    implementation(libs.appintro)

    // PhenoLib
    implementation(libs.phenolib)

    // Testing
    testImplementation(libs.junit)
    implementation(libs.androidx.test.ext.junit)
    implementation(libs.androidx.test.core)
    testImplementation(libs.mockito.core)
    testImplementation(libs.mockito.android)

    // Espresso
    androidTestImplementation(libs.espresso.core)
    androidTestImplementation(libs.espresso.contrib)
    androidTestImplementation(libs.espresso.intents)
    androidTestImplementation(libs.espresso.accessibility)
    androidTestImplementation(libs.espresso.web)
    androidTestImplementation(libs.espresso.idling.concurrent)
    androidTestImplementation(libs.espresso.idling.resource)

    // Navigation testing
    androidTestImplementation(libs.androidx.navigation.testing)
}
