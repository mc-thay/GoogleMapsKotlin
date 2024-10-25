plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.aguilar.googlemapskt"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.aguilar.googlemapskt"
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
    buildFeatures(){
        viewBinding=true
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    implementation ("com.google.android.libraries.places:places:4.0.0")
    implementation ("com.google.android.gms:play-services-maps:19.0.0")
    implementation ("com.google.maps:google-maps-services:0.18.0")
    implementation ("org.slf4j:slf4j-simple:1.7.25")
    implementation("com.google.maps.android:android-maps-utils:2.2.5")
    implementation("com.squareup.okhttp3:okhttp:4.10.0")
    implementation ("com.google.android.gms:play-services-location:21.3.0")



}