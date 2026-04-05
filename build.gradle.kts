import com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask

plugins {
    id("application")
    id("org.openjfx.javafxplugin") version "0.1.0"
    id("com.github.ben-manes.versions") version "0.53.0"
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

// ──────────────────────────────────────────────────────────────────────────
// Dependency locking — pins all transitive dependency versions for
// reproducible builds and supply-chain attack mitigation.
// To regenerate after any dependency change:
//   ./gradlew dependencies --write-locks --no-parallel
// ──────────────────────────────────────────────────────────────────────────

configurations.configureEach {
    // Lock all resolvable configurations (skips consumable/declarable ones).
    if (isCanBeResolved) {
        resolutionStrategy.activateDependencyLocking()
    }
}

// ──────────────────────────────────────────────────────────────────────────
// Dependency update checks via gradle-versions-plugin
// Run: ./gradlew dependencyUpdates --no-parallel
// ──────────────────────────────────────────────────────────────────────────

fun isNonStable(version: String): Boolean {
    val stableKeyword = listOf("RELEASE", "FINAL", "GA").any { version.uppercase().contains(it) }
    val regex = "^[0-9,.v-]+(-r)?$".toRegex()
    return !(stableKeyword || regex.matches(version))
}

tasks.withType<DependencyUpdatesTask> {
    // Reject non-stable candidate versions when the current version is stable
    rejectVersionIf {
        isNonStable(candidate.version) && !isNonStable(currentVersion)
    }
}

application {
    mainClass = "net.markwalder.picturetriage.Main"
    // JavaFX uses native code for graphics rendering on the underlying OS.
    // This flag is required to allow JavaFX to access native graphics libraries (OpenGL, Metal, etc.)
    applicationDefaultJvmArgs = listOf(
        "--enable-native-access=javafx.graphics"
    )
}

// ──────────────────────────────────────────────────────────────────────────
// Native app bundle via jpackage
// Run: ./gradlew jpackage  →  produces build/jpackage/Picture Triage-<ver>.dmg
//
// Three-step process required for non-modular JavaFX apps:
//   1. jlink    – creates a minimal JRE with JavaFX modules embedded
//   2. buildIcns – converts source PNGs into a macOS .icns icon file
//   3. jpackage – bundles the app JARs + JRE + icon into a native installer
//
// Change --type dmg to --type app-image to produce a .app bundle without
// the DMG wrapper (faster, but no installer).
// ──────────────────────────────────────────────────────────────────────────

// Step 1a – stage JavaFX modular JARs (from installDist) as jlink module path
val prepareJfxMods by tasks.registering(Copy::class) {
    group = "distribution"
    description = "Stages JavaFX JARs for jlink"
    dependsOn(tasks.named("installDist"))
    from(layout.buildDirectory.dir("install/${project.name}/lib"))
    include("javafx-*.jar")
    into(layout.buildDirectory.dir("jfx-mods"))
}

// Step 1b – stage app + dependency JARs without JavaFX (which is in the JRE)
val prepareAppLibs by tasks.registering(Copy::class) {
    group = "distribution"
    description = "Stages app and dependency JARs (excluding JavaFX) for jpackage"
    dependsOn(tasks.named("installDist"))
    from(layout.buildDirectory.dir("install/${project.name}/lib"))
    exclude("javafx-*.jar")
    into(layout.buildDirectory.dir("app-libs"))
}

// Step 2 – build a minimal JRE with the JavaFX modules embedded via jlink
// jlink resolves all transitive module dependencies automatically, so only
// the top-level JavaFX modules and jdk.unsupported need to be listed here.
val jlinkRuntime by tasks.registering(Exec::class) {
    group = "distribution"
    description = "Creates a minimal JRE with JavaFX modules using jlink"
    dependsOn(prepareJfxMods)
    val jreDir = layout.buildDirectory.dir("jre")
    outputs.dir(jreDir)
    doFirst { jreDir.get().asFile.deleteRecursively() }
    commandLine(
        "jlink",
        "--module-path", layout.buildDirectory.dir("jfx-mods").get().asFile.absolutePath,
        "--add-modules", "javafx.controls,javafx.swing,jdk.unsupported",
        "--output", jreDir.get().asFile.absolutePath,
        "--no-header-files",
        "--no-man-pages",
        "--strip-debug"
    )
}

// Step 3 – build the macOS .icns icon from the source PNG assets.
// iconutil requires an .iconset directory with specifically named files and
// then produces a single .icns file that jpackage can use.
val buildIcns by tasks.registering(Exec::class) {
    group = "distribution"
    description = "Converts source PNG icons into a macOS .icns file"
    val iconsetDir = layout.buildDirectory.dir("icons/PictureTriage.iconset")
    val icnsFile = layout.buildDirectory.file("icons/PictureTriage.icns")
    inputs.dir("src/main/resources/icons")
    outputs.file(icnsFile)
    doFirst {
        // Populate the .iconset directory with the required filenames.
        // Each logical size needs a 1x file and a @2x (retina) file at 2x resolution.
        val src = file("src/main/resources/icons")
        val dst = iconsetDir.get().asFile
        dst.deleteRecursively()
        dst.mkdirs()
        mapOf(
            "icon_16x16.png"    to "icon_16.png",
            "icon_16x16@2x.png" to "icon_32.png",
            "icon_32x32.png"    to "icon_32.png",
            "icon_32x32@2x.png" to "icon_64.png",
            "icon_128x128.png"  to "icon_128.png",
            "icon_128x128@2x.png" to "icon_256.png",
            "icon_256x256.png"  to "icon_256.png",
            "icon_256x256@2x.png" to "icon_512.png",
            "icon_512x512.png"  to "icon_512.png"
        ).forEach { (dest, source) ->
            src.resolve(source).copyTo(dst.resolve(dest), overwrite = true)
        }
    }
    commandLine(
        "iconutil",
        "--convert", "icns",
        "--output", icnsFile.get().asFile.absolutePath,
        iconsetDir.get().asFile.absolutePath
    )
}

// Step 4 – assemble the native installer
// jpackage requires a plain x.y.z version; strip any -SNAPSHOT qualifier.
val jpackage by tasks.registering(Exec::class) {
    group = "distribution"
    description = "Creates a native macOS .dmg installer using jpackage"
    dependsOn(jlinkRuntime, prepareAppLibs, buildIcns)
    val outDir = layout.buildDirectory.dir("jpackage")
    outputs.dir(outDir)
    doFirst { outDir.get().asFile.deleteRecursively() }
    val appVersion = project.version.toString().substringBefore("-")
    commandLine(
        "jpackage",
        "--runtime-image", layout.buildDirectory.dir("jre").get().asFile.absolutePath,
        "--input", layout.buildDirectory.dir("app-libs").get().asFile.absolutePath,
        "--main-jar", "${project.name}-${project.version}.jar",
        "--main-class", "net.markwalder.picturetriage.Main",
        "--java-options", "--enable-native-access=javafx.graphics",
        "--java-options", "--add-modules=javafx.controls,javafx.swing",
        "--name", "Picture Triage",
        "--app-version", appVersion,
        "--icon", layout.buildDirectory.file("icons/PictureTriage.icns").get().asFile.absolutePath,
        "--dest", outDir.get().asFile.absolutePath,
        "--type", "dmg"
    )
}
