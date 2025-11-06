import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.baselineprofile)
    alias(libs.plugins.sentry.android.gradle)
}

val propsFile = rootProject.file("sentry.properties")
val props = Properties()

if (propsFile.exists()) {
    props.load(propsFile.inputStream())
}

android {
    namespace = "me.proton.android.lumo"
    compileSdk = 36

    defaultConfig {
        minSdk = 29
        targetSdk = 36
        versionCode = 37
        versionName = "1.2.6"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Default production environment
        buildConfigField("String", "ENV_NAME", "\"\"")
        buildConfigField("String", "SENTRY_DSN", "\"${props["dsn"] ?: ""}\"")
    }

    signingConfigs {
        create("release") {
            keyAlias = System.getenv("LUMO_KEY_ALIAS") ?: "lumo"
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
            buildConfigField("String", "BASE_DOMAIN", "\"proton.me\"")
            buildConfigField("String", "OFFER_ID", "\"introductory-799\"")
        }

        try {
            configurePrivateFlavors()
        } catch (e: Throwable) {
            // Private flavors not available (public repo)
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
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:${libs.versions.lifecycleRuntimeKtx.get()}")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:${libs.versions.lifecycleRuntimeKtx.get()}")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:${libs.versions.lifecycleRuntimeKtx.get()}")
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation("androidx.compose.material:material-icons-core")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("com.google.android.material:material:1.13.0")
    implementation("io.coil-kt:coil-compose:2.7.0")
    implementation("com.airbnb.android:lottie-compose:6.6.9")
    implementation(libs.androidx.navigation.compose)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.androidx.profileinstaller)
    implementation(libs.android.startup.runtime)

    implementation("com.alphacephei:vosk-android:0.3.70@aar")
    implementation("net.java.dev.jna:jna:5.13.0@aar")

    implementation(project(":vosk-model"))

    "baselineProfile"(project(":baselineprofile"))

    "gmsImplementation"(libs.billing.ktx)

    // Hilt removed - using lightweight DependencyProvider instead

    testImplementation(libs.junit)
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.0")
    testImplementation("app.cash.turbine:turbine:1.1.0")
    testImplementation("io.mockk:mockk:1.13.11")
    testImplementation("io.mockk:mockk-android:1.13.11")
    testImplementation("androidx.arch.core:core-testing:2.2.0")
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}

sentry {
    org.set("proton")
    projectName.set("android-lumo")

    autoInstallation {
        autoUploadProguardMapping = isSentryAutoUploadEnabled()
        uploadNativeSymbols = isSentryAutoUploadEnabled()
    }

    tracingInstrumentation {
        enabled = false
    }
}

fun isSentryAutoUploadEnabled(): Boolean = gradle.startParameter.taskNames.any {
    it.contains("release", true)
}