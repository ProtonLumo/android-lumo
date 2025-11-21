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
