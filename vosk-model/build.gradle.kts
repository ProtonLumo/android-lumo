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

    sourceSets.named("main") {
        assets.srcDir(layout.buildDirectory.dir("generated/assets"))
    }

    buildTypes {
        create("alpha") {
            matchingFallbacks += listOf("debug")
        }
    }
}

tasks.register("genUUID") {
    val odir = layout.buildDirectory.dir("generated/assets/model-en-us")
    val ofile = odir.map { it.file("uuid") }

    outputs.file(ofile)

    doLast {
        val uuid = UUID.randomUUID().toString()
        val outputFile = ofile.get().asFile
        outputFile.parentFile.mkdirs()
        outputFile.writeText(uuid)
    }
}

tasks.named("preBuild") {
    dependsOn("genUUID")
}