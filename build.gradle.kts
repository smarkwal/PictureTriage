plugins {
    id("application")
    id("org.openjfx.javafxplugin") version "0.1.0"
}

repositories {
    mavenCentral()
}

javafx {
    version = "21.0.2"
    modules = listOf("javafx.controls")
}

application {
    mainClass = "net.markwalder.picturetriage.Main"
}
