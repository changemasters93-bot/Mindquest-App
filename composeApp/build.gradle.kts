import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties

// Load local.properties for signing credentials (gitignored)
val localProps = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) load(file.inputStream())
}

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.google.services)
    alias(libs.plugins.firebase.crashlytics)
    // SQLDelight plugin disabled: v2.2.1 native klibs require Kotlin 2.2.x ABI.
    // Offline cache is infrastructure-only, not wired to the main flow.
    // Re-enable when upgrading Kotlin to 2.2.x.
    // alias(libs.plugins.sqldelight)
}

kotlin {
    androidTarget {
        @OptIn(ExperimentalKotlinGradlePluginApi::class)
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }

    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "ComposeApp"
            isStatic = true
        }
    }

    sourceSets {
        commonMain.dependencies {
            // Compose
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation(compose.animation)

            // Navigation
            implementation(libs.navigation.compose)

            // Lifecycle
            implementation(libs.lifecycle.viewmodel.compose)
            implementation(libs.lifecycle.runtime.compose)

            // Supabase
            implementation(libs.supabase.gotrue)
            implementation(libs.supabase.postgrest)
            implementation(libs.supabase.realtime)
            implementation(libs.supabase.compose.auth)
            implementation(libs.supabase.compose.auth.ui)

            // Ktor
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.serialization.json)

            // Koin
            implementation(libs.koin.core)
            implementation(libs.koin.compose)
            // koin-compose-viewmodel removed: its IR is broken on iOS/Native
            // with Kotlin 2.1.0 + Compose 1.7.3 (causes SIGABRT).
            // ViewModels are registered as factory{} and retrieved via koinInject<T>().

            // Kotlinx
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.datetime)

            // Settings (key-value persistence)
            implementation(libs.multiplatform.settings.no.arg)

            // Coil
            implementation(libs.coil.compose)
            implementation(libs.coil.network.ktor)
        }

        androidMain.dependencies {
            implementation(libs.androidx.core.ktx)
            implementation(libs.androidx.activity.compose)
            implementation(libs.ktor.client.okhttp)
            implementation(libs.kotlinx.coroutines.android)
            implementation(libs.koin.android)
            // SQLDelight disabled: v2.2.1 klibs ABI incompatible with Kotlin 2.1.0
            // implementation(libs.sqldelight.coroutines)
            // implementation(libs.sqldelight.android.driver)
        }

        iosMain.dependencies {
            implementation(libs.ktor.client.darwin)
            // SQLDelight native driver excluded: v2.2.1 klibs require Kotlin 2.2.x ABI.
            // Offline cache is infrastructure-only (not wired to main flow yet).
            // Re-enable when upgrading Kotlin to 2.2.x.
        }
    }
}

android {
    namespace = "com.android.mindquest"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.android.mindquest"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"

        buildConfigField("String", "SUPABASE_URL", "\"${project.findProperty("SUPABASE_URL") ?: ""}\"")
        buildConfigField("String", "SUPABASE_ANON_KEY", "\"${project.findProperty("SUPABASE_ANON_KEY") ?: ""}\"")
    }

    signingConfigs {
        create("release") {
            storeFile = rootProject.file(localProps.getProperty("RELEASE_STORE_FILE", "mindquest-release.keystore"))
            storePassword = localProps.getProperty("RELEASE_STORE_PASSWORD", "")
            keyAlias = localProps.getProperty("RELEASE_KEY_ALIAS", "")
            keyPassword = localProps.getProperty("RELEASE_KEY_PASSWORD", "")
        }
    }

    buildTypes {
        debug {
            isDebuggable = true
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            signingConfig = signingConfigs.getByName("release")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "/META-INF/versions/9/previous-compilation-data.bin"
        }
    }
}

// ── Android variant-specific dependencies ────────────────────────────
dependencies {
    // Chucker: HTTP inspector (debug) / no-op (release)
    debugImplementation(libs.chucker.library)
    releaseImplementation(libs.chucker.noop)
    // Firebase (BOM must be in top-level dependencies, not KMP sourceSets)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.crashlytics)
}

// SQLDelight config disabled along with plugin (see plugins block comment).
// Re-enable when upgrading Kotlin to 2.2.x.
// sqldelight {
//     databases {
//         create("MindquestDatabase") {
//             packageName.set("com.android.mindquest.cache")
//         }
//     }
// }
