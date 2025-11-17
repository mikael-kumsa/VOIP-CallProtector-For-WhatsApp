plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("kotlin-parcelize")
}

android {
    namespace = "com.lal.voipcallprotector"
    compileSdk = 34

    lint {
        abortOnError = true
        checkReleaseBuilds = true
        warningsAsErrors = false
        disable.add("MissingTranslation")
        disable.add("ExtraTranslation")
    }

    defaultConfig {
        applicationId = "com.lal.voipcallprotector" 
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Production build config
        buildConfigField("String", "WHATSAPP_PACKAGE", "\"com.whatsapp\"")
        buildConfigField("String", "APP_VERSION", "\"${versionName}\"")

        // For Google Play
        resValue("string", "app_name", "VOIP Call Protector")
    }

    signingConfigs {
        // Read signing credentials from gradle.properties or environment variables
        val keystoreFile = project.findProperty("KEYSTORE_FILE") as String? 
            ?: System.getenv("KEYSTORE_FILE")
            ?: "app/release.keystore"
        val keystorePasswordValue = project.findProperty("KEYSTORE_PASSWORD") as String? 
            ?: System.getenv("KEYSTORE_PASSWORD")
        val keyAliasNameValue = project.findProperty("KEY_ALIAS") as String? 
            ?: System.getenv("KEY_ALIAS")
            ?: "voip-call-protector"
        val keyPasswordValue = project.findProperty("KEY_PASSWORD") as String? 
            ?: System.getenv("KEY_PASSWORD")
        
        // Only create signing config if keystore file exists and credentials are provided
        val keystoreFileObj = file(keystoreFile)
        if (keystoreFileObj.exists() && keystorePasswordValue != null && keyPasswordValue != null) {
            create("release") {
                storeFile = keystoreFileObj
                storePassword = keystorePasswordValue
                keyAlias = keyAliasNameValue
                keyPassword = keyPasswordValue
                println("Release signing configured with keystore: $keystoreFile")
            }
        } else {
            println("WARNING: Release keystore not found at '$keystoreFile' or credentials missing.")
            println("WARNING: Release builds will NOT be signed. Generate keystore using generate-keystore.bat")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            isDebuggable = false
            // Only use signing config if it exists
            val releaseSigningConfig = signingConfigs.findByName("release")
            if (releaseSigningConfig != null) {
                signingConfig = releaseSigningConfig
            } else {
                println("WARNING: Release build will be unsigned. Configure signing in gradle.properties")
            }
            proguardFiles(
                    getDefaultProguardFile("proguard-android-optimize.txt"),
                    "proguard-rules.pro"
            )
        }
        debug {
            isMinifyEnabled = false
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-DEBUG"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    kotlinOptions { jvmTarget = "1.8" }

    buildFeatures {
        viewBinding = true
        buildConfig = true
    }
}

dependencies {
    // Keep your existing dependencies
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.10.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.recyclerview:recyclerview:1.3.2")
    implementation("androidx.lifecycle:lifecycle-service:2.7.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.7.0")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.7.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    implementation("androidx.work:work-runtime-ktx:2.9.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")

    // Testing
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    implementation("com.google.android.gms:play-services-base:18.2.0")
    implementation("com.google.android.gms:play-services-basement:18.2.0")
    
    // Optional: Add safety net (helps with Play Protect)
    implementation("com.google.android.gms:play-services-safetynet:18.0.1")
}
