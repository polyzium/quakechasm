plugins {
    id("java")
    id("io.papermc.paperweight.userdev") version "1.5.11" // Check for new versions at https://plugins.gradle.org/plugin/io.papermc.paperweight.userdev
}

group = "ru.darkchronics"
version = "0.1"

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://maven.enginehub.org/repo/")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.20.4-R0.1-SNAPSHOT")
    compileOnly("dev.jorel:commandapi-bukkit-core:9.3.0")
    implementation("org.apache.commons:commons-lang3:3.14.0")
    implementation("com.sk89q.worldedit:worldedit-bukkit:7.2.18")
    paperweight.paperDevBundle("1.20.4-R0.1-SNAPSHOT")
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(17))
}

tasks.assemble {
    dependsOn(tasks.reobfJar)
}