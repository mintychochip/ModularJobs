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
    compileOnly(libs.paper.api)
    compileOnly(libs.mcmmo) {
        exclude(group = "com.sk89q.worldguard")
    }
    compileOnly(libs.lwc)
    compileOnly(libs.bolt)
    // Preferences public API only (soft-depend; not shaded into the plugin fat jar)
    compileOnly(libs.preferences.api)
    // JobPets API for pet change events (file dependency from parent workspace)
    compileOnly(files("../../jobpets-api/build/libs/jobpets-api-1.0.0.jar"))

    testImplementation("org.junit.jupiter:junit-jupiter:5.11.4")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    // MockBukkit for Paper 26.2 — mock server for OfflinePlayer / Material runtime
    testImplementation(libs.mockbukkit)
    testImplementation(libs.paper.api)
    // Preferences API for unit tests of the bridge (still not packaged into shadowJar)
    testImplementation(libs.preferences.api)
    // PostgreSQL only — driver ships in the plugin artifact
    implementation(libs.postgresql)
    testImplementation(libs.postgresql)
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
        // Paper version line is 26.2 (post-1.21.11 renumbering); requires Java 25
        minecraftVersion("26.2")
        downloadPlugins {
            // Bolt 1.2.x lists Paper 26.2 compatibility on Hangar
            hangar("Bolt", "1.2.22")
        }
    }
}
