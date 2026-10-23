plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.google.services)
    id("com.google.devtools.ksp")
}

android {
    namespace = "com.ada.gyselmode"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.ada.gyselmode"
        minSdk = 34
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildFeatures {
        viewBinding = true
    }


    buildTypes {
        release {
            optimization {
                enable = false
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation(libs.androidx.activity.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.junit.ktx)
    implementation(libs.androidx.ui.test)
    implementation(libs.material)
    testImplementation(libs.junit)
    //androidTestImplementation(libs.androidx.espresso.core)
    //androidTestImplementation(libs.androidx.junit)

    //firebased
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)

    //Image
    implementation("io.coil-kt:coil:2.7.0")

    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")

    // Login con Google
    implementation("androidx.credentials:credentials:1.3.0")
    implementation("androidx.credentials:credentials-play-services-auth:1.3.0")
    implementation("com.google.android.libraries.identity.googleid:googleid:1.1.1")

    //MAPS

    implementation("org.maplibre.gl:android-sdk:11.8.0")
    implementation("com.google.android.gms:play-services-location:21.3.0")


    //DATABASE ROOM
    implementation("androidx.room:room-runtime:2.8.5")
    ksp("androidx.room:room-compiler:2.8.5")
    implementation("androidx.room:room-ktx:2.8.5")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.1")

    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.0")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.8.0")


//Firestore
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)

    implementation("com.google.firebase:firebase-firestore")


//DISEÑO - ACTIVIDAD
    implementation("androidx.viewpager2:viewpager2:1.1.0")


    //carga de imagenes
    implementation("com.github.bumptech.glide:glide:4.16.0")



}