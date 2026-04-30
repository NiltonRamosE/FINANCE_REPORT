import java.util.Properties
import java.io.FileInputStream

val localProperties = Properties()
val localPropertiesFile = rootProject.file("local.properties")

if (localPropertiesFile.exists()) {
    localProperties.load(FileInputStream(localPropertiesFile))
}

val supabaseUrl: String = localProperties.getProperty("SUPABASE_URL")
    ?: "https://fallback.supabase.co"

val supabaseAnonKey: String = localProperties.getProperty("SUPABASE_ANON_KEY")
    ?: "fallback_key"

val googleWebClientId: String = localProperties.getProperty("GOOGLE_WEB_CLIENT_ID")
    ?: "web_client_id"

val googleAndroidClientId: String = localProperties.getProperty("GOOGLE_ANDROID_CLIENT_ID")
    ?: "android_client_id"

val keystorePath: String = localProperties.getProperty("KEYSTORE_PATH")
    ?: "keystore_path"

val keystorePassword: String = localProperties.getProperty("KEYSTORE_PASSWORD")
    ?: "keystore_password"

val keyAliasVal: String = localProperties.getProperty("KEY_ALIAS")
    ?: "key_alias"

val keyPasswordVal: String = localProperties.getProperty("KEY_PASSWORD")
    ?: "key_password"

val cloudinaryCloudName: String = localProperties.getProperty("CLOUDINARY_CLOUD_NAME")
    ?: "cloudinaryCloudName"

val cloudinaryUploadPreset: String = localProperties.getProperty("CLOUDINARY_UPLOAD_PRESET")
    ?: "cloudinaryUploadPreset"

val cloudinaryApiKey: String = localProperties.getProperty("CLOUDINARY_API_KEY")
    ?: "cloudinaryCloudName"

val cloudinaryApiSecret: String = localProperties.getProperty("CLOUDINARY_API_SECRET")
    ?: "cloudinaryUploadPreset"

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.kapt)
    alias(libs.plugins.hilt)
    alias(libs.plugins.kotlin.parcelize)
    id("com.google.gms.google-services")
}

android {
    namespace = "com.nramos.finance_report"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.nramos.finance_report"
        minSdk = 29
        targetSdk = 36
        versionCode = 1
        versionName = "1.2.13"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField("String", "SUPABASE_URL", "\"$supabaseUrl\"")
        buildConfigField("String", "SUPABASE_ANON_KEY", "\"$supabaseAnonKey\"")
        buildConfigField("String", "GOOGLE_WEB_CLIENT_ID", "\"$googleWebClientId\"")
        buildConfigField("String", "GOOGLE_ANDROID_CLIENT_ID", "\"$googleAndroidClientId\"")
        buildConfigField("String", "VERSION_NAME", "\"${versionName}\"")
        buildConfigField("String", "CLOUDINARY_CLOUD_NAME", "\"${cloudinaryCloudName}\"")
        buildConfigField("String", "CLOUDINARY_UPLOAD_PRESET", "\"${cloudinaryUploadPreset}\"")
        buildConfigField("String", "CLOUDINARY_API_KEY", "\"${cloudinaryApiKey}\"")
        buildConfigField("String", "CLOUDINARY_API_SECRET", "\"${cloudinaryApiSecret}\"")
    }

    buildFeatures {
        viewBinding = true
        buildConfig = true
    }

    signingConfigs {
        create("release") {
            storeFile = file(keystorePath)
            storePassword = keystorePassword
            keyAlias = keyAliasVal
            keyPassword = keyPasswordVal
        }

        getByName("debug") {
            storeFile = file(keystorePath)
            storePassword = keystorePassword
            keyAlias = keyAliasVal
            keyPassword = keyPasswordVal
        }
    }

    buildTypes {
        debug {
            signingConfig = signingConfigs.getByName("debug")
        }
        release {
            signingConfig = signingConfigs.getByName("release")
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
}

dependencies {

    // Core
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)

    // Lifecycle
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.lifecycle.livedata.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)

    // Navigation
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)

    // Coroutines
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.coroutines.core)

    // Retrofit
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.gson)
    implementation(libs.okhttp.logging.interceptor)
    implementation(libs.okhttp)

    // DataStore
    implementation(libs.androidx.datastore.preferences)

    // Dagger Hilt
    implementation(libs.hilt.android)
    kapt(libs.hilt.compiler)

    // Gson
    implementation(libs.gson)

    // Glide
    implementation(libs.glide)

    // Google Sign-In
    implementation("com.google.android.gms:play-services-auth:21.0.0")

    // Supabase client (para la autenticación)
    implementation("io.github.jan-tennert.supabase:gotrue-kt:2.0.0")
    implementation("io.ktor:ktor-client-android:2.3.7")

    // Para manejar JWT
    implementation("io.jsonwebtoken:jjwt-api:0.12.5")
    runtimeOnly("io.jsonwebtoken:jjwt-impl:0.12.5")
    runtimeOnly("io.jsonwebtoken:jjwt-orgjson:0.12.5")

    // Image Picker
    implementation("com.github.dhaval2404:imagepicker:2.1")

    // Glide para cargar imágenes
    implementation("com.github.bumptech.glide:glide:4.16.0")

    // Cloudinary (para subir imágenes)
    implementation("com.cloudinary:cloudinary-android:2.3.1")

    // Permissions (si necesitas cámara)
    implementation("com.karumi:dexter:6.2.3")

    implementation("de.hdodenhof:circleimageview:3.1.0")

    implementation("com.github.PhilJay:MPAndroidChart:v3.1.0")

    // Firebase
    implementation(platform("com.google.firebase:firebase-bom:32.8.0"))
    implementation("com.google.firebase:firebase-messaging:23.4.1")
    implementation("com.google.firebase:firebase-analytics:21.6.2")

    // WorkManager para notificaciones programadas
    implementation("androidx.work:work-runtime-ktx:2.9.0")
    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}

kapt {
    correctErrorTypes = true
}