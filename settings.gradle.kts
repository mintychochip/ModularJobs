
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
        // Sibling SNAPSHOT: exclusive so Gradle 9.7 does not fail the whole
        // resolve when a remote (GitHub Packages 401, incendo DNS) errors on
        // maven-metadata.xml. Publish with:
        //   ./gradlew -p ../databag publishAllPublicationsToLocalBuildRepoRepository
        exclusiveContent {
            forRepository {
                maven {
                    name = "databagLocal"
                    url = uri(rootDir.resolve("../databag/build/maven-repo"))
                }
            }
            filter {
                includeGroup("dev.mintychochip.databag")
            }
        }
        exclusiveContent {
            forRepository {
                maven {
                    name = "conditionsLocal"
                    url = uri(rootDir.resolve("../conditions/build/maven-repo"))
                }
            }
            filter {
                includeGroup("dev.conditions")
            }
        }
        mavenLocal()
        mavenCentral()
        maven("https://repo.papermc.io/repository/maven-public/")
        maven("https://jitpack.io")
        maven("https://repo.codemc.io/repository/maven-public/")
        maven("https://nexus.neetgames.com/repository/maven-releases/")
        maven("https://repo.extendedclip.com/content/repositories/placeholderapi/")
    }
}