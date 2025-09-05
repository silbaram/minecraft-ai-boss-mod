import org.gradle.api.JavaVersion
import org.gradle.kotlin.dsl.extra

// Apply the NeoForge userdev plugin and the Kotlin JVM plugin.  The
// userdev plugin sets up the Minecraft development environment, while
// the Kotlin plugin allows you to write your mod in Kotlin.
plugins {
    id("net.neoforged.gradle.userdev") version "7.0.140"
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
    maven(url = "https://maven.neoforged.net/snapshots")
    maven(url = "https://libraries.minecraft.net")
}

// 런타임 중 ASM 모듈/클래스패스 충돌을 방지하기 위해 ASM 버전을 정렬합니다.
configurations.all {
    resolutionStrategy.eachDependency {
        if (requested.group == "org.ow2.asm") {
            useVersion("9.7")
            because("Align ASM to 9.7 to avoid module path/classpath conflicts")
        }
    }
}

dependencies {
    // Pull in the NeoForge API matching the version defined in gradle.properties.
    implementation("net.neoforged:neoforge:${project.property("neo_version")}")
    // Kotlin 표준 라이브러리 (런타임 포함)
    implementation(kotlin("stdlib"))
    runtimeOnly(kotlin("stdlib"))
    runtimeOnly("org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.9.22")
    // ONNX Runtime allows running lightweight neural networks for tactic
    // selection.  This dependency pulls in native binaries at runtime.
    implementation("com.microsoft.onnxruntime:onnxruntime:1.19.2")
}

// Configure Kotlin compilation to target the same JVM version as Minecraft.
tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions.jvmTarget = "21"
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

// Dev 편의를 위해 runClient 실행 전 Kotlin 표준 라이브러리 JAR을 개발 런 디렉터리의 mods 폴더에 복사합니다.
tasks.matching { it.name == "runClient" }.configureEach {
    doFirst {
        val kotlinJars = configurations.runtimeClasspath.get().files.filter { it.name.startsWith("kotlin-stdlib") }
        val modsDir = layout.projectDirectory.dir("runs/client/mods").asFile
        modsDir.mkdirs()
        kotlinJars.forEach { jar ->
            copy {
                from(jar)
                into(modsDir)
            }
        }
    }
}
