plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.google.gms.google.services)
}

android {
    namespace = "kgz.senior.dishkit"
    compileSdk = 36

    defaultConfig {
        applicationId = "kgz.senior.dishkit"
        minSdk = 24
        targetSdk = 36
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
            signingConfig = signingConfigs.getByName("debug")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    // Стандартные библиотеки
    implementation("androidx.appcompat:appcompat:1.6.1") // Здесь были круглые скобки вокруг строки
    implementation("com.google.android.material:material:1.11.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.recyclerview:recyclerview:1.3.2")

    // ViewModel и LiveData
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.7.0")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.7.0")

    implementation(platform("com.google.firebase:firebase-bom:32.7.2"))
    implementation("com.google.firebase:firebase-analytics")
    implementation("com.google.firebase:firebase-auth")    // Аутентификация
    implementation("com.google.firebase:firebase-firestore") // База данных
    implementation("com.google.firebase:firebase-storage")  // Хранилище файлов

    implementation("com.github.bumptech.glide:glide:4.16.0")
    implementation(libs.activity)
    annotationProcessor("com.github.bumptech.glide:compiler:4.16.0")

}