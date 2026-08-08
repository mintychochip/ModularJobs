rootProject.name = "jobs2"

include("api", "common", "paper")

// Preferences API (live 0.1.0): resolve via mavenLocal (./gradlew -p ../preferences :api:publishToMavenLocal)
// and/or GitHub Packages below. Composite includeBuild of ../preferences is optional — when
// present it can be re-enabled, but mavenLocal is the reliable consumer path under memory pressure.
// includeBuild("../preferences") {
//     dependencySubstitution {
//         substitute(module("dev.jlo:preferences-api")).using(project(":api"))
//     }
// }

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)

    repositories {
        // MockBukkit Paper 26.2 line (vendored until Central publishes mockbukkit-v26.2)
        maven {
            name = "mockbukkitLocal"
            url = uri(rootDir.resolve("libs/mockbukkit-maven"))
        }
        // craftux published jars from sibling checkout (./gradlew -p ../craftux publish)
        maven {
            name = "craftuxLocal"
            url = uri(rootDir.resolve("../craftux/build/maven-repo"))
        }
        // Mint durable ledger API from sibling checkout (./gradlew -p ../mint publish)
        maven {
            name = "mintLocal"
            url = uri(rootDir.resolve("../mint/build/maven-repo"))
        }
        mavenLocal()
        // GitHub Packages: dev.jlo:preferences-api (needs read:packages when composite unavailable)
        maven {
            name = "GitHubPackagesPreferences"
            url = uri("https://maven.pkg.github.com/mintychochip/Preferences")
            credentials {
                username = System.getenv("GITHUB_ACTOR")
                    ?: (settings.providers.gradleProperty("gpr.user").orNull)
                    ?: ""
                password = System.getenv("GITHUB_TOKEN")
                    ?: (settings.providers.gradleProperty("gpr.key").orNull)
                    ?: ""
            }
        }
        mavenCentral()
        maven("https://repo.papermc.io/repository/maven-public/")
        maven("https://jitpack.io")
        maven("https://repo.codemc.io/repository/maven-public/")
        maven("https://nexus.neetgames.com/repository/maven-releases/")
        maven("https://repo.extendedclip.com/content/repositories/placeholderapi/")
        // optional; DNS sometimes fails — content also on GitLab above
        maven("https://repo.incendo.org/releases")
    }
}