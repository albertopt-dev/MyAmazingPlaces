plugins {
    alias(libs.plugins.android.application) // Plugin para aplicaciones Android
    alias(libs.plugins.kotlin.android) // Plugin para Kotlin Android
    alias(libs.plugins.google.services)
    alias(libs.plugins.google.android.libraries.mapsplatform.secrets.gradle.plugin) // Plugin para Firebase Google Services
}

android {
    namespace = "edu.albertoperez.myamazingplaces1"
    compileSdk = 35

    defaultConfig {
        applicationId = "edu.albertoperez.myamazingplaces1"
        minSdk = 21
        targetSdk = 35
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
    buildFeatures {
        viewBinding = true
    }
}

dependencies {
    // Core AndroidX Libraries
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.constraintlayout)

    // Navigation Component
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)

    // Lifecycle and Fragment Libraries
    implementation(libs.androidx.activity)
    implementation(libs.androidx.fragment.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)

    // RecyclerView
    implementation(libs.androidx.recyclerview)

    // Gson for JSON Parsing
    implementation(libs.gson)

    // Glide for Image Loading
    implementation(libs.glide)

    // Firebase Libraries
    implementation(libs.firebase.firestore.ktx) // Firestore
    implementation(libs.firebase.storage.ktx) // Firebase Storage
    implementation(libs.firebase.analytics.ktx)
    implementation(libs.play.services.maps)
    implementation(libs.androidx.core) // Analytics

    // Test Libraries
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    //Temas
    implementation (libs.material.v190)
    implementation (libs.androidx.appcompat.v161)

    //Mapas
    implementation (libs.play.services.maps.v1802)
    implementation (libs.play.services.location)
    implementation (libs.play.services.maps)
    implementation(libs.play.services.location.license)
}
