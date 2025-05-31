plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.secrets)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.gms.google.services)
    alias(libs.plugins.firebase.crashlytics)
}

android {
    namespace = "com.xwurfel.tourry"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.xwurfel.tourry"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Firebase configuration
        resValue("string", "firebase_emulator_mode", "false")

        // Add any other configuration values
        buildConfigField("boolean", "ENABLE_FIREBASE_LOGGING", "false")
        buildConfigField("boolean", "ENABLE_ANALYTICS", "true")

        manifestPlaceholders["firebase_analytics_collection_deactivated"] = false
        manifestPlaceholders["MAPS_API_KEY"] = project.findProperty("MAPS_API_KEY") ?: ""
    }

    buildTypes {
        debug {
            isMinifyEnabled = false
            isDebuggable = true

            // Firebase debug configuration
            resValue("string", "firebase_emulator_mode", "true")
            buildConfigField("boolean", "ENABLE_FIREBASE_LOGGING", "true")
            buildConfigField("boolean", "ENABLE_ANALYTICS", "true")

            // Enable Firestore offline persistence in debug
            buildConfigField("boolean", "FIRESTORE_PERSISTENCE_ENABLED", "true")
        }

        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )

            // Firebase release configuration
            buildConfigField("boolean", "ENABLE_FIREBASE_LOGGING", "false")
            buildConfigField("boolean", "ENABLE_ANALYTICS", "true")
            buildConfigField("boolean", "FIRESTORE_PERSISTENCE_ENABLED", "true")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlinOptions {
        jvmTarget = "11"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    secrets {
        propertiesFileName = "secrets.properties"
        defaultPropertiesFileName = "local.defaults.properties"
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    // Core Android dependencies
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.androidx.window)
    implementation(libs.androidx.material3.window.size.class1)

    // Coroutines
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.coroutines.play.services)

    // Serialization
    implementation(libs.kotlinx.serialization.json)

    // Maps
    implementation(libs.maps.compose)
    implementation(libs.maps.compose.utils)
    implementation(libs.maps.compose.widgets)
    implementation(libs.maps.utils.ktx)

    // Dependency Injection
    implementation(libs.hilt.android)
    implementation(libs.androidx.hilt.navigation.compose)
    implementation(libs.hilt.work)
    ksp(libs.hilt.compiler)

    // Permissions
    implementation(libs.accompanist.permissions)

    // Location Services
    implementation(libs.play.services.location)

    // Database
    implementation(libs.androidx.room.common)
    implementation(libs.androidx.room.ktx)
    implementation(libs.androidx.room.runtime)
    ksp(libs.androidx.room.compiler)

    // Image Loading
    implementation(libs.coil.compose)

    // Fonts
    implementation(libs.androidx.ui.text.google.fonts)

    // Networking
    implementation(libs.retrofit)
    implementation(libs.converter.gson)
    implementation(libs.retrofit2.kotlinx.serialization.converter)
    implementation(libs.okhttp)
    implementation(libs.logging.interceptor)

    // Security
    implementation(libs.androidx.security.state)
    implementation(libs.androidx.security.crypto)

    // Work Manager
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.androidx.lifecycle.service)

    // Data Storage
    implementation(libs.androidx.datastore.preferences)

    // Logging
    implementation(libs.timber)

    // Firebase - Use BOM for version management
    implementation(platform(libs.firebase.bom))

    // Firebase Authentication
    implementation(libs.firebase.auth)
    implementation(libs.play.services.auth)

    // Firestore
    implementation(libs.firebase.firestore)

    // Firebase Storage
    implementation(libs.firebase.storage.ktx)

    // Firebase Analytics
    implementation(libs.firebase.analytics.ktx)

    // Firebase Cloud Messaging
    implementation(libs.firebase.messaging.ktx)

    // Firebase Crashlytics
    implementation(libs.firebase.crashlytics.ktx)

    // Firebase Cloud Functions (if needed)
    implementation(libs.firebase.functions.ktx)

    // Firebase Realtime Database (if needed for real-time features)
    implementation(libs.firebase.database)

    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)

    // Debug tools
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}