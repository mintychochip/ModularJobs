plugins {
    id("xyz.jpenilla.run-paper") version "2.3.1"
    id("com.github.johnrengelman.shadow") version "8.1.1"
    java
    `java-library`
}

repositories {
    mavenCentral()
    maven("https://repo.incendo.org/releases")
    maven("https://repo.extendedclip.com/content/repositories/placeholderapi/")
    maven("https://nexus.neetgames.com/repository/maven-releases/")
    maven("https://repo.codemc.io/repository/maven-public/")
    maven("https://jitpack.io")
    maven("https://repo.papermc.io/repository/maven-public/")
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
}

dependencies {
    implementation("com.esotericsoftware:kryo:5.6.2")
    implementation("com.google.inject:guice:7.0.0")
    implementation(project(":jobs-api"))
    implementation(libs.undertow.core)
    implementation(libs.exp4j)
    implementation(libs.hikaricp)
    implementation(libs.caffeine)
    compileOnly("com.github.mintychochip:preferences:631142d")
//    compileOnly("io.lumine:Mythic-Dist:5.6.1") {
//        exclude(group="org.jetbrains.annotations")
//    }
    compileOnly("me.clip:placeholderapi:2.11.6")
    compileOnly("org.jetbrains:annotations:24.1.0")
    compileOnly(libs.paper.api)
    compileOnly(libs.vault.api) {
        exclude(group = "org.bukkit")
    }
    compileOnly(libs.mcmmo) {
        exclude(group = "com.sk89q.worldguard")
    }
    compileOnly(libs.lwc)
    compileOnly(libs.bolt)
}

tasks {
    shadowJar {
        mergeServiceFiles()
    }

    build {
        dependsOn(shadowJar)
    }

    named<xyz.jpenilla.runpaper.task.RunServer>("runServer") {
        val toolchains = project.extensions.getByType<JavaToolchainService>()
        javaLauncher.set(
            toolchains.launcherFor {
                languageVersion.set(JavaLanguageVersion.of(21))
            }
        )
        minecraftVersion("1.21.7")
        downloadPlugins {
            url("https://github.com/EssentialsX/Essentials/releases/download/2.21.2/EssentialsX-2.21.2.jar")
            hangar("Bolt","1.1.78")
        }

    }
}
