plugins {
    id("xyz.jpenilla.run-paper") version "2.3.1"
    id("com.github.johnrengelman.shadow") version "8.1.1"
    java
    `java-library`
}

group = "org.aincraft"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven("https://repo.codemc.io/repository/maven-public/")
    maven("https://jitpack.io")
    maven("https://repo.papermc.io/repository/maven-public/")
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
}

dependencies {
    implementation("net.objecthunter:exp4j:0.4.8")
    implementation("com.zaxxer:HikariCP:5.0.1")
    compileOnly("io.papermc.paper:paper-api:1.21.4-R0.1-SNAPSHOT")
    compileOnly("com.github.MilkBowl:VaultAPI:1.7") {
        exclude(group = "org.bukkit")
    }
    compileOnly("com.griefcraft:lwc:2.4.0")
    compileOnly("org.popcraft:bolt-bukkit:1.1.52")
    compileOnly("com.github.lokka30.treasury:treasury-api:2.0.1") {
        exclude(group = "io.papermc.paper")
    }
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
            url("https://www.spigotmc.org/resources/vault.34315/download?version=344916")
            url("https://github.com/EssentialsX/Essentials/releases/download/2.21.2/EssentialsX-2.21.2.jar")
            url("https://www.spigotmc.org/resources/lwc-extended.69551/download?version=557109")
        }
    }
}
