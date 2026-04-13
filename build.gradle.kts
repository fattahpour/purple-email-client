import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.gradle.api.tasks.bundling.Zip

plugins {
    kotlin("jvm") version "2.1.0"
    id("org.jetbrains.compose") version "1.7.1"
    id("org.jetbrains.kotlin.plugin.compose") version "2.1.0"
    java
}

group = "com.project"
version = "1.0.3"

val appName = "purple-email-client"
val releaseJarsDir = layout.buildDirectory.dir("release-jars")

fun currentOsClassifier(): String {
    val os = System.getProperty("os.name").lowercase().let {
        when {
            it.contains("win") -> "windows"
            it.contains("mac") -> "macos"
            it.contains("linux") -> "linux"
            else -> it.replace(Regex("[^a-z0-9]+"), "-").trim('-')
        }
    }
    val arch = System.getProperty("os.arch").lowercase().let {
        when (it) {
            "x86_64", "amd64" -> "x64"
            "aarch64", "arm64" -> "arm64"
            else -> it.replace(Regex("[^a-z0-9]+"), "-").trim('-')
        }
    }
    return "$os-$arch"
}

repositories {
    mavenCentral()
    google()
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
}

dependencies {
    // Compose Desktop runtime + Material 3
    implementation(compose.desktop.currentOs)
    implementation(compose.material3)

    // Coroutines — swing dispatcher is needed for Dispatchers.Main in a Compose Desktop/Skiko+AWT app
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-swing:1.10.1")

    // JavaMail
    implementation("com.sun.mail:javax.mail:1.6.2")

    // Tests — JUnit 5
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.10.1")
}

compose.desktop {
    application {
        mainClass = "com.project.emailclient.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = appName
            packageVersion = project.version.toString()
        }
    }
}

/**
 * Builds a self-contained fat JAR for the current OS.
 *
 * Compose Desktop fat JARs contain native Skiko libraries, so they are
 * OS-specific. Release builds must publish the classifier in the file name
 * instead of renaming every platform artifact to the same generic JAR name.
 *
 * Usage: ./gradlew packageCurrentOsFatJar
 * Run:   java -jar build/release-jars/purple-email-client-<os>-<arch>-1.0.3.jar
 */
tasks.register<Copy>("packageCurrentOsFatJar") {
    dependsOn("packageUberJarForCurrentOS")
    from(layout.buildDirectory.dir("compose/jars")) {
        include("*.jar")
    }
    into(releaseJarsDir)
    doLast {
        println("OS-specific fat JAR copied to ${releaseJarsDir.get().asFile}")
    }
}

/**
 * Backwards-compatible local convenience task. Prefer packageCurrentOsFatJar
 * for release artifacts so the OS/architecture classifier stays visible.
 */
tasks.register<Copy>("packageFatJar") {
    dependsOn("packageCurrentOsFatJar")
    from(releaseJarsDir) {
        include("$appName-*-${project.version}.jar")
        rename { "$appName.jar" }
    }
    into(layout.buildDirectory.dir("libs"))
    doLast {
        println("Convenience fat JAR -> ${layout.buildDirectory.get().asFile}/libs/$appName.jar")
    }
}

/**
 * Builds a portable application directory with a bundled Java runtime and zips
 * it for distribution. This is the safest artifact for computers that do not
 * already have a compatible Java installation.
 */
tasks.register<Zip>("packagePortableDistribution") {
    dependsOn("createDistributable")
    archiveFileName.set("$appName-${currentOsClassifier()}-${project.version}-portable.zip")
    destinationDirectory.set(layout.buildDirectory.dir("distributions"))
    from(layout.buildDirectory.dir("compose/binaries/main/app/$appName")) {
        into(appName)
    }
}

val launcherScriptsDir = layout.buildDirectory.dir("generated/launcher-scripts")

val createLauncherScripts = tasks.register("createLauncherScripts") {
    dependsOn("packageCurrentOsFatJar")
    outputs.dir(launcherScriptsDir)
    doLast {
        val jarName = releaseJarsDir.get().asFile
            .listFiles { file -> file.name.matches(Regex("$appName-.*-${project.version}\\.jar")) }
            ?.firstOrNull()
            ?.name
            ?: error("Could not find OS-specific JAR in ${releaseJarsDir.get().asFile}")

        val dir = launcherScriptsDir.get().asFile
        dir.mkdirs()
        dir.resolve("run-windows.bat").writeText(
            """
            @echo off
            setlocal
            cd /d "%~dp0"
            java -jar "$jarName"
            pause
            
            """.trimIndent()
        )
        dir.resolve("run-linux-macos.sh").writeText(
            """
            #!/usr/bin/env sh
            cd "$(dirname "$0")" || exit 1
            exec java -jar "$jarName"
            
            """.trimIndent()
        )
    }
}

/**
 * Packages the current OS fat JAR with simple launch scripts. On Windows this
 * produces a ZIP containing the Windows fat JAR and run-windows.bat.
 */
tasks.register<Zip>("packageCurrentOsJarBundle") {
    dependsOn("packageCurrentOsFatJar", createLauncherScripts)
    archiveFileName.set("$appName-${currentOsClassifier()}-${project.version}-jar-bundle.zip")
    destinationDirectory.set(layout.buildDirectory.dir("distributions"))

    from(releaseJarsDir) {
        include("$appName-*-${project.version}.jar")
        into(appName)
    }
    from(launcherScriptsDir) {
        into(appName)
    }
}

// Both Java 17 and Kotlin 2.1 target the same JVM release.
java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(17))
}
kotlin {
    jvmToolchain(17)
}

tasks.test {
    useJUnitPlatform()
}
