plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "ch.widmedia.Eintrag"
    compileSdk = 36

    defaultConfig {
        applicationId = "ch.widmedia.Eintrag"
        minSdk = 36
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    kotlinOptions {
        jvmTarget = "21"
    }

    buildFeatures {
        viewBinding = true
    }
}

tasks.register("prepareKotlinBuildScriptModel") { }

dependencies {
    implementation(libs.sqlcipher)
    implementation(libs.sqlite)
    implementation(libs.material)
    implementation(libs.appcompat)
    implementation(libs.recyclerview)
}
