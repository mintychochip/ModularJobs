
rootProject.name = "jobs2"

include("api", "common", "paper")



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
        // databag (./gradlew -p ../databag publishAllPublicationsToLocalBuildRepoRepository)
        maven {
            name = "databagLocal"
            url = uri(rootDir.resolve("../databag/build/maven-repo"))
        }
        maven {
            name = "databagGitHub"
            url = uri("https://maven.pkg.github.com/mintychochip/databag")
            credentials {
                username = System.getenv("GITHUB_ACTOR") ?: ""
                password = System.getenv("GITHUB_TOKEN") ?: ""
            }
        }
        mavenLocal()
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