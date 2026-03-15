import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "kr.co.kworks.socket_server_test"
    compileSdk = 35

    defaultConfig {
        applicationId = "kr.co.kworks.socket_server_test"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        create("release") {
            storeFile = file(getProperty("STORE_FILE_PATH"))
            storePassword = getProperty("STORE_PASSWORD")
            keyAlias = getProperty("STORE_KEY_ALIAS")
            keyPassword = getProperty("STORE_KEY_PASSWORD")
        }
    }


    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release");
            buildConfigField("Boolean", "IS_PRODUCTION", "false")
            isDebuggable = false
        }


        debug {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            buildConfigField("Boolean", "IS_PRODUCTION", "false")
        }
    }

    sourceSets {
        getByName("main").res.srcDir("src/main/res/layouts/common")
        getByName("main").res.srcDir("src/main/res/layouts/activity")
        getByName("main").res.srcDir("src/main/res/layouts/item")
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    buildFeatures {
        dataBinding = true
        buildConfig = true
    }
}

dependencies {
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
}

fun loadLocalProperties(file: File): Properties {
    val properties = Properties()
    if (file.exists()) {
        file.inputStream().use { properties.load(it) }
    }
    return properties
}

fun getProperty(key: String): String {
    val localProperties = loadLocalProperties(rootProject.file("local.properties"))
    return localProperties.getProperty(key)
}