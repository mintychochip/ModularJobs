rootProject.name = "jobs2"

include("jobs-api", "jobs-core")

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)

    repositories {
        mavenCentral()
        maven("https://repo.papermc.io/repository/maven-public/")
        maven("https://jitpack.io")
        maven("https://repo.codemc.io/repository/maven-public/")
        maven("https://nexus.neetgames.com/repository/maven-releases/")
        maven("https://repo.extendedclip.com/content/repositories/placeholderapi/")
        maven("https://repo.incendo.org/releases")
        maven("https://gitlab.com/api/v4/projects/77453344/packages/maven")
    }
}