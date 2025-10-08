import org.gradle.api.JavaVersion
import org.gradle.api.file.DuplicatesStrategy
import org.gradle.kotlin.dsl.extra

plugins {
    id("net.neoforged.gradle.userdev") version "7.0.140"
    kotlin("jvm") version "2.1.21"
    kotlin("plugin.serialization") version "2.1.21"
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
    // ONNX Runtime (CPU)
    runtimeOnly("com.microsoft.onnxruntime:onnxruntime:1.19.2")
    compileOnly("com.microsoft.onnxruntime:onnxruntime:1.19.2")
    // JSON serialization for logging
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")

    // Tests
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.10.0")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.10.0")
    testImplementation(kotlin("test"))
    testImplementation(kotlin("test-junit5"))
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    compilerOptions.jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
}

tasks.test {
    useJUnitPlatform()
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

// Shadow jar only for distribution build (-Pdist=true); dev runs use normal classpath
tasks.shadowJar {
    archiveClassifier.set("fat")
    configurations = listOf(project.configurations.runtimeClasspath.get())
    relocate("ai.onnxruntime", "com.github.silbaram.bossai.shaded.onnxruntime")
    enabled = project.findProperty("dist") == "true"
}

tasks.jar {
    // Produce thin jar in dev; if dist build, depend on shadow
    archiveClassifier.set("")
    if (project.findProperty("dist") == "true") {
        dependsOn(tasks.shadowJar)
    }
}

// Configure runClient if present (NeoForge userdev supplies it). Use a safe lookup.
tasks.matching { it.name == "runClient" }.configureEach {
    if (this is JavaExec) {
        systemProperty("boss_ai.dev.env", "true")
        systemProperty("file.encoding", "UTF-8")

        // locale 설정 전달 우선순위:
        // 1) JVM 시스템 속성(-Dboss_ai.logging.locale)
        // 2) Gradle 프로젝트 속성(-Pboss_ai.logging.locale 또는 gradle.properties의 루트 키)
        // 3) 환경변수(BOSS_AI_LOGGING_LOCALE)
        // 4) 기본값(en)
        val localeProp = System.getProperty("boss_ai.logging.locale")?.lowercase()
            ?: (project.findProperty("boss_ai.logging.locale") as String?)?.lowercase()
            ?: System.getenv("BOSS_AI_LOGGING_LOCALE")?.lowercase()
            ?: "en"
        systemProperty("boss_ai.logging.locale", localeProp)

        // Add ONNX Runtime JAR to bootclasspath for NeoForge module system
        val onnxJar = configurations.runtimeClasspath.get().find { it.name.contains("onnxruntime") }
        if (onnxJar != null) {
            jvmArgs("-Dfile.encoding=UTF-8", "-Xbootclasspath/a:${onnxJar.absolutePath}")
        } else {
            jvmArgs("-Dfile.encoding=UTF-8")
        }
        classpath += configurations.runtimeClasspath.get()
    }
}

