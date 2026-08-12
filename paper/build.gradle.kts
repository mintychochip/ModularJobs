plugins {
    alias(libs.plugins.run.paper)
    alias(libs.plugins.shadow)
}

dependencies {
    implementation(libs.kryo)
    implementation(libs.guava)
    implementation(project(":api"))
    implementation(project(":common"))
    implementation(libs.exp4j)
    implementation(libs.hikaricp)
    implementation(libs.caffeine)
    implementation(libs.gson)
    implementation(libs.configurate.core)
    // craftux multi-surface UI (replaces triumph-gui).
    // craftux-paper's published JAR already embeds api+common classes; depend only
    // on paper and exclude its POM transitive to avoid double-shading duplicates.
    implementation(libs.craftux.paper) {
        isTransitive = false
    }
    // Compile against api/common source jars via the paper artifact coordinates
    // is enough for the IDE/compiler because paper embeds those types.
    compileOnly("io.github.flog99:mapgui-api:1.0.0")
    compileOnly(libs.placeholderapi)
    compileOnly(libs.jetbrains.annotations)
    compileOnly(libs.paper.api)
    compileOnly(libs.mcmmo) {
        exclude(group = "com.sk89q.worldguard")
    }
    compileOnly(libs.lwc)
    compileOnly(libs.bolt)

    testImplementation("org.junit.jupiter:junit-jupiter:5.11.4")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    // MockBukkit for Paper 26.2 — mock server for OfflinePlayer / Material runtime
    testImplementation(libs.mockbukkit)
    testImplementation(libs.paper.api)
    // PostgreSQL only — driver ships in the plugin artifact
    implementation(libs.postgresql)
    testImplementation(libs.postgresql)
}
val descriptorVersion = project.version.toString()
tasks.processResources {
    filesMatching("plugin.yml") {
        expand("version" to descriptorVersion)
    }
}

tasks.test {
    useJUnitPlatform()
}

tasks {
    shadowJar {
        mergeServiceFiles()
        // Drop craftux-paper demo plugin/config descriptors (keep ModularJobs ones)
        exclude { details ->
            val fromCraftux = details.file.absolutePath.contains("craftux-")
            fromCraftux && (details.name == "plugin.yml" || details.name == "config.yml")
        }
        relocate("dev.craftux", "net.aincraft.libs.craftux")
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
            // 2.12.3+ required for Paper 26.2 (2.11.x crashes parsing version "26.2")
            hangar("PlaceholderAPI", "2.12.3")
        }
    }
}
