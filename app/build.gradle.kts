import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.baselineprofile)
    alias(libs.plugins.sentry.android.gradle)
    alias(libs.plugins.google.hilt)
    alias(libs.plugins.ksp)
    alias(libs.plugins.kotlin.serialization)
    id("version-tasks")
}

val versionPropsFile = rootProject.file("version.properties")
val versionProps = Properties().apply { load(versionPropsFile.inputStream()) }

val localPropsFile = rootProject.file("local.properties")
    .takeIf { it.exists() }
    ?.inputStream()
    ?.use { Properties().apply { load(it) } }

fun prop(name: String, default: String): String {
    return System.getenv(name)
        ?: localPropsFile?.getProperty(name)
        ?: default
}

val sentryPropsFile = rootProject.file("sentry.properties")
val sentryProps = Properties()
if (sentryPropsFile.exists()) {
    sentryProps.load(sentryPropsFile.inputStream())
}
fun sentryProp(name: String): String {
    return sentryProps.getProperty(name) ?: ""
}

android {
    namespace = "me.proton.android.lumo"
    compileSdk = 36

    defaultConfig {
        minSdk = 29
        targetSdk = 36
        versionName = "1.2.11"
        versionCode = 43

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Default production environment
        buildConfigField("String", "ENV_NAME", "\"\"")
        buildConfigField("String", "SENTRY_DSN", "\"${sentryProp("dsn")}\"")

        ndk {
            abiFilters += listOf(
                "armeabi-v7a",
                "arm64-v8a",
                "x86",
                "x86_64"
            )
        }
    }

    signingConfigs {
        create("release") {
            keyAlias = System.getenv("LUMO_KEY_ALIAS") ?: ""
            keyPassword = System.getenv("LUMO_KEY_PASSWORD")
            storeFile = System.getenv("LUMO_KEYSTORE_PATH")?.let { file(it) }
            storePassword = System.getenv("LUMO_STORE_PASSWORD")
        }
    }

    buildTypes {
        debug {
            signingConfig = signingConfigs.getByName("debug")
        }
        release {
            signingConfig = signingConfigs.getByName("release")
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        create("alpha") {
            signingConfig = signingConfigs.getByName("debug")
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        create("benchmarkRelease") {
            signingConfig = signingConfigs.getByName("debug")
        }
        create("nonMinifiedRelease") {
            signingConfig = signingConfigs.getByName("debug")
        }
    }

    flavorDimensions += listOf("env", "services")
    productFlavors {
        create("gms") {
            dimension = "services"
            versionNameSuffix = "-gms"
        }
        create("noGms") {
            dimension = "services"
            versionNameSuffix = "-nogms"
        }

        create("production") {
            dimension = "env"
            applicationId = "me.proton.android.lumo"
            buildConfigField(
                "String",
                "BASE_DOMAIN",
                "\"${prop("BASE_DOMAIN_PRODUCTION", "proton.me")}\""
            )
            buildConfigField("String", "OFFER_ID", "\"${prop("OFFER_ID_PRODUCTION", "")}\"")
        }

        create("noble") {
            dimension = "env"
            applicationId = "me.proton.lumo"
            buildConfigField("String", "BASE_DOMAIN", "\"${prop("BASE_DOMAIN_NOBLE", "")}\"")
            buildConfigField("String", "OFFER_ID", "\"${prop("OFFER_ID_NOBLE", "")}\"")
        }
    }


    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlin {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    // Optimize build performance
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "/META-INF/DEPENDENCIES"
        }
    }

    // Custom APK naming
    applicationVariants.all {
        val variant = this
        variant.outputs.all {
            val output = this as com.android.build.gradle.internal.api.BaseVariantOutputImpl
            val appName = "lumo"
            val versionName = variant.versionName
            val buildType = variant.buildType.name
            val flavor = variant.flavorName

            // Format: lumo-v0.1.2-production-debug.apk
            output.outputFileName = "${appName}-v${versionName}-${flavor}-${buildType}.apk"
        }
    }

    dependenciesInfo {
        // Disables dependency metadata when building APKs.
        includeInApk = false
        // Disables dependency metadata when building Android App Bundles.
        includeInBundle = false
    }

    hilt {
        enableAggregatingTask = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.compose.material.icons.core)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.android.google.material)
    implementation(libs.coil.compose)
    implementation(libs.lottie)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.androidx.profileinstaller)
    implementation(libs.android.startup.runtime)

    implementation(libs.hilt.android)
    implementation(libs.hilt.navigation.compose)
    ksp(libs.hilt.compiler)

    implementation("com.alphacephei:vosk-android:0.3.70@aar")
    implementation("net.java.dev.jna:jna:5.18.1@aar")

    implementation(project(":vosk-model"))

    "baselineProfile"(project(":baselineprofile"))

    "gmsImplementation"(libs.billing.ktx)
    "gmsImplementation"(libs.sentry)
    "gmsImplementation"(libs.sentry.android)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
    testImplementation(libs.mockk)
    testImplementation(libs.mockk.android)
    testImplementation(libs.androidx.core.testing)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}

sentry {
    autoInstallation {
        enabled = !isNoGms()
        sentryVersion = libs.versions.sentry.asProvider()
        autoUploadProguardMapping = isSentryAutoUploadEnabled()
        uploadNativeSymbols = isSentryAutoUploadEnabled()
    }

    tracingInstrumentation {
        enabled = false
    }

    ignoredVariants.set(
        listOf(
            "productionNoGmsDebug",
            "nobleNoGmsDebug",
            "productionNoGmsAlpha",
            "nobleNoGmsAlpha",
            "productionNoGmsRelease",
            "nobleNoGmsRelease",
        )
    )
}

fun isNoGms(): Boolean = gradle.startParameter.taskNames.any {
    it.contains("noGms", true)
}

fun isSentryAutoUploadEnabled(): Boolean = gradle.startParameter.taskNames.any {
    it.contains("productionGmsRelease", true)
}