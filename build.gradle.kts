import org.gradle.api.JavaVersion
import org.gradle.kotlin.dsl.extra

// Apply the NeoForge userdev plugin and the Kotlin JVM plugin.  The
// userdev plugin sets up the Minecraft development environment, while
// the Kotlin plugin allows you to write your mod in Kotlin.
plugins {
    id("net.neoforged.gradle.userdev") version "7.0.120"
    kotlin("jvm") version "1.9.22"
}

// Use values from gradle.properties for group and version.  This ensures
// your published JAR uses your mod's metadata【840203598704741†L150-L170】.
group = project.property("mod_group_id") as String
version = project.property("mod_version") as String

java {
    // Set the Java toolchain to the version recommended by NeoForge.
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
}

// Configure the repositories used to resolve dependencies.  NeoForge is
// published to its own Maven, while Kotlin and ONNX Runtime are available
// from Maven Central.
repositories {
    mavenCentral()
    maven(url = "https://maven.neoforged.net/releases")
}

dependencies {
    // Pull in the NeoForge API matching the version defined in gradle.properties.
    implementation("net.neoforged:neoforge:${project.property("neo_version")}")
    // ONNX Runtime allows running lightweight neural networks for tactic
    // selection.  This dependency pulls in native binaries at runtime.
    implementation("com.microsoft.onnxruntime:onnxruntime:1.19.2")
}

// Configure Kotlin compilation to target the same JVM version as Minecraft.
tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions.jvmTarget = "21"
}