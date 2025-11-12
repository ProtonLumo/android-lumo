plugins {
    `kotlin-dsl`
}

repositories {
    google()
    mavenCentral()
}

gradlePlugin {
    plugins {
        register("version-tasks") {
            id = "version-tasks"
            implementationClass = "VersionTasksPlugin"
        }
    }
}

dependencies {
    implementation("com.android.tools.build:gradle-api:8.11.1")
}
