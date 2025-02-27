plugins {
    id("kotlin-kapt")
    alias(libs.plugins.android.application)
    alias(libs.plugins.jetbrains.kotlin.android)
    alias(libs.plugins.hilt.android)
    alias(libs.plugins.navigation.safeargs)
    alias(libs.plugins.google.services)
}

android {
    namespace = "com.aliaktas.urbanscore"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.aliaktas.urbanscore"
        minSdk = 24
        targetSdk = 34
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
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        viewBinding = true
        buildConfig = true
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.swiperefreshlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    //View
    implementation(libs.androidx.cardview)
    //RecyclerView
    implementation(libs.androidx.recyclerview)
    //Navigation Component
    implementation(libs.navigation.fragment.ktx)
    implementation(libs.navigation.ui.ktx)
    //ViewModel
    implementation(libs.lifecycle.viewmodel.ktx)
    implementation(libs.lifecycle.livedata.ktx)
    //Room
    implementation(libs.room.runtime)
    kapt(libs.room.compiler)
    implementation(libs.room.ktx)
    // Retrofit
    implementation(libs.retrofit)
    implementation(libs.retrofit.gson)
    // Glide
    implementation(libs.glide)
    kapt(libs.glide.compiler)
    // Hilt
    implementation(libs.hilt.android)
    kapt(libs.hilt.compiler)
    //Coroutines
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.coroutines.core)
    //Gson
    implementation (libs.gson)
    // Firebase
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore)
    //implementation(libs.firebase.analytics)
    //implementation(libs.firebase.messaging)

    //Lottie Animations
    implementation(libs.lottie)


}