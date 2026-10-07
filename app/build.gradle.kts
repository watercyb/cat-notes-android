import org.gradle.kotlin.dsl.annotationProcessor
import org.gradle.kotlin.dsl.implementation
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    id("com.google.devtools.ksp") version "2.3.6"
}

android {
    signingConfigs {
        getByName("debug") {
            storeFile = file(project.findProperty("KEYSTORE_FILE_DEBUG") as String)
            storePassword = project.findProperty("KEYSTORE_PASSWORD_DEBUG") as String
            keyAlias = project.findProperty("KEY_ALIAS_DEBUG") as String
            keyPassword = project.findProperty("KEY_PASSWORD_DEBUG") as String
        }
        create("release") {
            storeFile = file(project.findProperty("KEYSTORE_FILE_RELEASE") as String)
            storePassword = project.findProperty("KEYSTORE_PASSWORD_RELEASE") as String
            keyAlias = project.findProperty("KEY_ALIAS_RELEASE") as String
            keyPassword = project.findProperty("KEY_PASSWORD_RELEASE") as String
        }
    }
    namespace = "com.blueberryjoy.notes"

    buildFeatures {
        buildConfig = true
        compose = true
    }

    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.blueberryjoy.notes"
        minSdk = 26
        targetSdk = 37
        versionCode = 8
        versionName = "1.2.1"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        debug {
            isDebuggable = true
        }
        release {
            manifestPlaceholders += mapOf()
            isMinifyEnabled = true
            isShrinkResources = true

            ndk {
                debugSymbolLevel = "SYMBOL_TABLE"
            }

            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
        javaParameters.set(true)
    }
}
configurations.all {
    resolutionStrategy {
        force("org.jetbrains:annotations:23.0.0")
    }
}
dependencies {
    implementation(libs.androidx.activity.ktx)
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(libs.volley)
    implementation(libs.lifecycle.viewmodel.ktx)
    implementation(libs.lifecycle.runtime.ktx)
    implementation(libs.activity.compose)
    implementation(platform(libs.compose.bom))
    implementation(libs.ui)
    implementation(libs.ui.graphics)
    implementation(libs.ui.tooling.preview)
    implementation(libs.material3)
    implementation(libs.room.runtime)
    implementation(libs.androidx.swiperefreshlayout)
    ksp(libs.room.compiler)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
    implementation("com.clerk:clerk-android-api:1.0.39") {
        exclude(group = "com.intellij", module = "annotations")
    }
    implementation("com.clerk:clerk-android-ui:1.0.39") {
        exclude(group = "com.intellij", module = "annotations")
    }
    androidTestImplementation(platform(libs.compose.bom))
    androidTestImplementation(libs.ui.test.junit4)
    implementation(libs.glide)
    annotationProcessor(libs.compiler)
    implementation(libs.accompanist.systemuicontroller)
    implementation(libs.retrofit)
    implementation(libs.converter.gson)
    implementation(libs.kotlinx.coroutines.android)
    debugImplementation(libs.ui.tooling)
    debugImplementation(libs.ui.test.manifest)
}