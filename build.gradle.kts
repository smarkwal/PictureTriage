plugins {
    id("application")
    id("org.openjfx.javafxplugin") version "0.1.0"
}

repositories {
    mavenCentral()
}

javafx {
    version = "25.0.1"
    modules = listOf("javafx.controls", "javafx.swing")
}

dependencies {
    implementation("com.github.usefulness:webp-imageio:0.10.0")
}

application {
    mainClass = "net.markwalder.picturetriage.Main"
}
