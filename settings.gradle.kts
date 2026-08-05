rootProject.name = "jobs2"

include("jobs-api", "jobs-core")

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)

    repositories {
        // MockBukkit Paper 26.2 line (vendored until Central publishes mockbukkit-v26.2)
        maven {
            name = "mockbukkitLocal"
            url = uri(rootDir.resolve("libs/mockbukkit-maven"))
        }
        mavenCentral()
        maven("https://repo.papermc.io/repository/maven-public/")
        // mint-api (before broken/unreachable mirrors)
        maven("https://gitlab.com/api/v4/projects/77453344/packages/maven")
        maven("https://jitpack.io")
        maven("https://repo.codemc.io/repository/maven-public/")
        maven("https://nexus.neetgames.com/repository/maven-releases/")
        maven("https://repo.extendedclip.com/content/repositories/placeholderapi/")
        // optional; DNS sometimes fails — content also on GitLab above
        maven("https://repo.incendo.org/releases")
    }
}