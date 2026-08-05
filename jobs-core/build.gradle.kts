plugins {
    alias(libs.plugins.run.paper)
    alias(libs.plugins.shadow)
}

dependencies {
    implementation(libs.kryo)
    implementation(libs.guava)
    implementation(project(":jobs-api"))
    implementation(libs.exp4j)
    implementation(libs.hikaricp)
    implementation(libs.caffeine)
    implementation(libs.gson)
    implementation(libs.configurate.core)
    implementation(libs.triumph.gui)

    compileOnly(libs.placeholderapi)
    compileOnly(libs.jetbrains.annotations)
    compileOnly(libs.mint.api)
    compileOnly(libs.paper.api)
    compileOnly(libs.mcmmo) {
        exclude(group = "com.sk89q.worldguard")
    }
    compileOnly(libs.lwc)
    compileOnly(libs.bolt)
    // JobPets API for pet change events (file dependency from parent workspace)
    compileOnly(files("../../jobpets-api/build/libs/jobpets-api-1.0.0.jar"))

    testImplementation("org.junit.jupiter:junit-jupiter:5.11.4")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    // MockBukkit for Paper 26.2 — mock server for OfflinePlayer / Material runtime
    testImplementation(libs.mockbukkit)
    testImplementation(libs.paper.api)
    // SQLite for production-SQL timed boost repository tests
    testImplementation("org.xerial:sqlite-jdbc:3.47.2.0")
}

tasks.test {
    useJUnitPlatform()
}

tasks {
    shadowJar {
        mergeServiceFiles()
        relocate("dev.triumphteam.gui", "net.aincraft.libs.triumphgui")
    }

    build {
        dependsOn(shadowJar)
    }

    named<xyz.jpenilla.runpaper.task.RunServer>("runServer") {
        val toolchains = project.extensions.getByType<JavaToolchainService>()
        javaLauncher.set(
            toolchains.launcherFor {
                languageVersion.set(JavaLanguageVersion.of(25))
            }
        )
        minecraftVersion("1.21.11")
        downloadPlugins {
            hangar("Bolt","1.1.78")
            hangar("Mint","1.4.0")
        }

    }
}
