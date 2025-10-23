plugins {
    id("java")
    id("io.papermc.paperweight.userdev") version "2.0.0-beta.19" // Check for new versions at https://plugins.gradle.org/plugin/io.papermc.paperweight.userdev
    id("com.gradleup.shadow") version "9.2.2"
}

group = "com.github.polyzium"
version = "1.0.0-alpha"

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://maven.enginehub.org/repo/")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21.8-R0.1-SNAPSHOT")
    compileOnly("dev.jorel:commandapi-paper-core:11.0.0")
    implementation("org.apache.commons:commons-lang3:3.14.0")
    compileOnly("com.sk89q.worldedit:worldedit-bukkit:7.2.18")
    implementation("fr.mrmicky:fastboard:2.1.5")
    paperweight.paperDevBundle("1.21.8-R0.1-SNAPSHOT")
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
}

tasks.assemble {
    dependsOn(tasks.reobfJar)
}