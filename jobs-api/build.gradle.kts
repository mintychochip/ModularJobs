plugins {
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
    compileOnly(libs.paper.api)
    compileOnly(libs.vault.api) {
        exclude(group = "org.bukkit")
    }
    compileOnly(libs.mcmmo) {
        exclude(group = "com.sk89q.worldguard")
    }
}
