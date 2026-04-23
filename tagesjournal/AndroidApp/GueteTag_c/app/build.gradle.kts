
plugins {
    id("com.android.application")
    kotlin("android")
    kotlin("kapt")
}
android {
    namespace = "ch.widmedia.guetetag"
    compileSdk = 36
    defaultConfig {
        applicationId = "ch.widmedia.guetetag"
        minSdk = 36
        targetSdk = 36
    }
    buildFeatures { compose = true }
    composeOptions { kotlinCompilerExtensionVersion = "2.0.0" }
    kotlinOptions { jvmTarget = "21" }
}
dependencies {
    implementation(platform("androidx.compose:compose-bom:2025.02.00"))
    implementation("androidx.compose.material3:material3")
    implementation("androidx.activity:activity-compose:1.10.1")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.security:security-crypto:1.1.0-alpha07")
    implementation("androidx.biometric:biometric:1.4.0")
    implementation("androidx.room:room-runtime:2.7.1")
    kapt("androidx.room:room-compiler:2.7.1")
    implementation("androidx.room:room-ktx:2.7.1")
    implementation("net.zetetic:sqlcipher-android:4.6.0")
    implementation("androidx.sqlite:sqlite-ktx:2.4.0")
}
