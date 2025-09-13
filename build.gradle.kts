import org.gradle.api.JavaVersion
import org.gradle.api.file.DuplicatesStrategy
import org.gradle.kotlin.dsl.extra

// Apply the NeoForge userdev plugin and the Kotlin JVM plugin. The
// userdev plugin sets up the Minecraft development environment, while
// the Kotlin plugin allows you to write your mod in Kotlin.
plugins {
    id("net.neoforged.gradle.userdev") version "7.0.140"
    kotlin("jvm") version "2.1.21"
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

group = project.property("mod_group_id") as String
version = project.property("mod_version") as String

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
}

repositories {
    mavenCentral()
    maven(url = "https://maven.neoforged.net/releases")
    maven(url = "https://maven.neoforged.net/snapshots")
    maven(url = "https://libraries.minecraft.net")
    maven {
        name = "Kotlin for Forge"
        url = uri("https://thedarkcolour.github.io/KotlinForForge/")
    }
}

configurations.all {
    resolutionStrategy.eachDependency {
        if (requested.group == "org.ow2.asm") {
            useVersion("9.7")
            because("Align ASM to 9.7 to avoid module path/classpath conflicts")
        }
    }
}

dependencies {
    implementation("net.neoforged:neoforge:${project.property("neo_version")}")
    implementation("thedarkcolour:kotlinforforge-neoforge:5.9.0")
    // ONNX Runtime (CPU) - use runtimeOnly for NeoForge module system
    runtimeOnly("com.microsoft.onnxruntime:onnxruntime:1.19.2")
    // Also include as compileOnly for IDE/compilation
    compileOnly("com.microsoft.onnxruntime:onnxruntime:1.19.2")
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    compilerOptions.jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
}

tasks.processResources {
    val props = mapOf(
        "mod_id" to project.property("mod_id"),
        "mod_version" to project.property("mod_version"),
        "mod_name" to project.property("mod_name"),
        "mod_authors" to project.property("mod_authors"),
        "mod_description" to project.property("mod_description"),
        "mod_license" to project.property("mod_license"),
        "minecraft_version_range" to project.property("minecraft_version_range"),
        "neo_version_range" to project.property("neo_version_range"),
    )
    inputs.properties(props)
    filesMatching("META-INF/neoforge.mods.toml") {
        expand(props)
    }
}


// Shadow jar only for distribution build (-Pdist=true); dev runs use normal classpath
tasks.shadowJar {
    archiveClassifier.set("fat")
    configurations = listOf(project.configurations.runtimeClasspath.get())
    relocate("ai.onnxruntime", "com.example.bossai.shaded.onnxruntime")
    enabled = project.findProperty("dist") == "true"
}

tasks.jar {
    // Produce thin jar in dev; if dist build, depend on shadow
    archiveClassifier.set("")
    if (project.findProperty("dist") == "true") {
        dependsOn(tasks.shadowJar)
    }
}


// Configure Kotlin compilation to target the same JVM version as Minecraft.
tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    compilerOptions.jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
}

// Replace tokens in neoforge.mods.toml with gradle.properties values.
tasks.processResources {
    val props = mapOf(
        "mod_id" to project.property("mod_id"),
        "mod_version" to project.property("mod_version"),
        "mod_name" to project.property("mod_name"),
        "mod_authors" to project.property("mod_authors"),
        "mod_description" to project.property("mod_description"),
        "mod_license" to project.property("mod_license"),
        "minecraft_version_range" to project.property("minecraft_version_range"),
        "neo_version_range" to project.property("neo_version_range"),
    )
    inputs.properties(props)
    filesMatching("META-INF/neoforge.mods.toml") {
        expand(props)
    }
}

// Configure runClient if present (NeoForge userdev supplies it). Use a safe lookup.
tasks.matching { it.name == "runClient" }.configureEach {
    if (this is JavaExec) {
        systemProperty("boss_ai.dev.env", "true")
        // Add ONNX Runtime JAR to bootclasspath for NeoForge module system
        val onnxJar = configurations.runtimeClasspath.get().find { it.name.contains("onnxruntime") }
        if (onnxJar != null) {
            jvmArgs("-Xbootclasspath/a:${onnxJar.absolutePath}")
        }
        classpath += configurations.runtimeClasspath.get()
    }
}

