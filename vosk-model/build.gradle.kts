import java.util.UUID

plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "me.proton.android.lumo.vosk"
    compileSdk = 36

    buildFeatures {
        buildConfig = false
    }

    buildTypes {
        create("alpha") {
            initWith(getByName("release"))
        }
    }
}
