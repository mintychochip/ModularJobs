plugins {
    alias(libs.plugins.run.paper)
    alias(libs.plugins.shadow)
}

dependencies {
    implementation(libs.kryo)
    implementation(libs.guice)
    implementation(project(":jobs-api"))
    implementation(libs.undertow.core)
    implementation(libs.exp4j)
    implementation(libs.hikaricp)
    implementation(libs.caffeine)
    implementation(libs.gson)

    compileOnly(libs.preferences)
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
        minecraftVersion("1.21.11")
        downloadPlugins {
            hangar("Bolt","1.1.78")
            hangar("Mint","1.3.0-52ae81b")
        }

    }
}
