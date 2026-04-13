import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    kotlin("jvm") version "2.1.0"
    id("org.jetbrains.compose") version "1.7.1"
    id("org.jetbrains.kotlin.plugin.compose") version "2.1.0"
    java
}

group = "com.project"
version = "0.0.1-SNAPSHOT"

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
            packageName = "purple-email-client"
            packageVersion = "1.0.0"
        }
    }
}

/**
 * Convenience task: builds a self-contained fat JAR that bundles all runtime
 * dependencies (including Skiko native libs for the current OS) and copies it
 * to build/libs/purple-email-client.jar.
 *
 * Usage: ./gradlew packageFatJar
 * Run:   java -jar build/libs/purple-email-client.jar
 */
tasks.register<Copy>("packageFatJar") {
    dependsOn("packageUberJarForCurrentOS")
    from(layout.buildDirectory.dir("compose/jars")) {
        include("*.jar")
        rename { "purple-email-client.jar" }
    }
    into(layout.buildDirectory.dir("libs"))
    doLast {
        println("Fat JAR → ${layout.buildDirectory.get().asFile}/libs/purple-email-client.jar")
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
