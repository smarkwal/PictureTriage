plugins {
    id("application")
    id("org.openjfx.javafxplugin") version "0.1.0"
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(25))
    }
}

tasks.withType<JavaCompile>().configureEach {
    // Emit Java 25 bytecode regardless of host JDK defaults.
    options.release.set(25)
}

tasks.withType<JavaExec>().configureEach {
    // Ensure 'run' also launches with Java 25.
    javaLauncher.set(
        javaToolchains.launcherFor {
            languageVersion.set(JavaLanguageVersion.of(25))
        }
    )
}

repositories {
    mavenCentral()
}

javafx {
    version = "25.0.1"
    modules = listOf("javafx.controls", "javafx.swing")
}

dependencies {
    // TwelveMonkeys ImageIO WebP plugin: Pure Java implementation for reading WebP images.
    implementation("com.twelvemonkeys.imageio:imageio-webp:3.13.1")
}

application {
    mainClass = "net.markwalder.picturetriage.Main"
}
