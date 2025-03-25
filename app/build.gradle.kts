plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.example.sonarappv2"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.sonarappv2"
        minSdk = 23
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

    // ✅ Step 2: Force GraphView and all dependencies to use AndroidX
    buildFeatures {
        viewBinding = true
    }

    packagingOptions {
        exclude("META-INF/proguard/androidx-annotations.pro")
    }
}

dependencies {

    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)

    // 🔹 Step 2: Ensure GraphView uses AndroidX only
    implementation("com.jjoe64:graphview:4.2.2") {
        exclude(group = "com.android.support") // ✅ Exclude old support libraries
    }
}
