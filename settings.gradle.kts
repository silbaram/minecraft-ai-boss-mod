// Configure plugin management repositories.  NeoForge provides its plugins
// through its own Maven repository.  This ensures Gradle can resolve the
// NeoGradle plugin used in build.gradle.kts【265535692119596†L40-L52】.
pluginManagement {
    repositories {
        // Default repositories
        mavenCentral()
        gradlePluginPortal()
        // NeoForge releases
        maven(url = "https://maven.neoforged.net/releases")
    }
}

// Set the name of this Gradle project.  This is used when generating the
// compiled JAR and other artefacts【528335682123187†L116-L121】.
rootProject.name = "boss-ai"