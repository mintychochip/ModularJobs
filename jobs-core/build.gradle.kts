plugins {
    id("xyz.jpenilla.run-paper") version "2.3.1"
    id("com.github.johnrengelman.shadow") version "8.1.1"
    java
    `java-library`
}

repositories {
    mavenCentral()
    maven("https://nexus.neetgames.com/repository/maven-releases/")
    maven("https://repo.codemc.io/repository/maven-public/")
    maven("https://jitpack.io")
    maven("https://repo.papermc.io/repository/maven-public/")
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
}

dependencies {
    implementation("com.google.inject:guice:7.0.0")
    implementation(project(":jobs-api"))
    implementation(libs.undertow.core)
    implementation(libs.exp4j)
    implementation(libs.hikaricp)
    implementation(libs.caffeine)

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
            url("https://www.spigotmc.org/resources/lwc-extended.69551/download?version=557109")
            url("https://www.spigotmc.org/resources/vault.34315/download?version=344916")
        }

    }
}
