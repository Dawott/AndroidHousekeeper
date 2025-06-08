
plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.example.trackerwydatkow"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.trackerwydatkow"
        minSdk = 27
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
/*
        javaCompileOptions {
            annotationProcessorOptions {
                arguments = ['room.schemaLocation': "$projectDir/schemas", 'room.incremental':"true"]
            }
        }*/
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
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
}



dependencies {
    val room_version = "2.6.1"

    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
    implementation("com.github.AppDevNext:AndroidChart:3.1.0.25")
    implementation("androidx.room:room-runtime:$room_version")
    annotationProcessor("androidx.room:room-compiler:$room_version")
}