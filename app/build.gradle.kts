import java.io.FileInputStream
import java.util.Properties

val localProps = Properties()
val localPropsFile = File(rootDir, "local.properties")
if (localPropsFile.exists()) {
    localProps.load(FileInputStream(localPropsFile))
    println("✔️ Loaded local.properties")
} else {
    println("⚠️ local.properties file not found")
}

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.hilt)
    alias(libs.plugins.devtools)
    alias(libs.plugins.google.gms.google.services)
    id("org.jetbrains.kotlin.plugin.serialization")
}

android {
    namespace = "dadm.grupo.dadmproyecto"
    compileSdk = 35

    defaultConfig {
        applicationId = "dadm.grupo.dadmproyecto"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField(
            "String",
            "SUPABASE_URL",
            "\"${localProps.getProperty("SUPABASE_URL", "default_url")}\""
        )
        buildConfigField(
            "String",
            "SUPABASE_KEY",
            "\"${localProps.getProperty("SUPABASE_KEY", "default_key")}\""
        )

        println("👉 SUPABASE_URL = ${localProps.getProperty("SUPABASE_URL")}")
        println("👉 SUPABASE_KEY = ${localProps.getProperty("SUPABASE_KEY")}")
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

    kotlinOptions {
        jvmTarget = "11"
    }

    buildFeatures {
        viewBinding = true
        buildConfig = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.firebase.auth)
    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play.services.auth)
    implementation(libs.googleid)
    implementation(libs.firebase.firestore.ktx)
    implementation(libs.play.services.location)
    implementation(libs.androidx.core.animation)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    implementation(libs.hilt.lib)
    ksp(libs.hilt.compiler)

    implementation(libs.androidx.navigation.ui.ktx)
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.swiperefreshlayout)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)

    implementation(libs.moshi.kotlin)
    implementation(libs.moshi.kotlin.codegen)
    implementation(libs.retrofit)
    implementation(libs.converter.moshi)

    implementation(libs.androidx.preference.ktx)
    implementation(libs.androidx.datastore.preferences)

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.roomcompiler)

    implementation(platform(libs.firebase.bom))
    implementation(libs.google.firebase.auth)

    implementation(libs.play.services.auth)

    implementation(libs.android.sdk)
    implementation(libs.android.plugin.annotation.v9)

    // Cliente Supabase para Kotlin
    implementation(libs.postgrest.kt)
    implementation(libs.realtime.kt)
    implementation(libs.storage.kt)
    implementation(libs.gotrue.kt)

    // Serialización (necesario para algunos módulos)
    implementation(libs.kotlinx.serialization.json)

    // HTTP client (Ktor)
    implementation(libs.ktor.client.android)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.serialization.kotlinx.json)

    implementation(libs.coil)

    implementation(libs.taptargetview)


    implementation(libs.glide)
    ksp(libs.glide.compiler)
}
