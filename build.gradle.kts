import com.github.spotbugs.snom.Confidence
import com.github.spotbugs.snom.Effort
import com.github.spotbugs.snom.SpotBugsExtension
import com.github.spotbugs.snom.SpotBugsTask
import net.ltgt.gradle.errorprone.errorprone
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.Task
import org.gradle.api.publish.maven.MavenPublication


plugins {
    alias(libs.plugins.errorprone) apply false
    alias(libs.plugins.spotbugs) apply false
    alias(libs.plugins.spotless) apply false
}

group = "org.aincraft"
// Canonical date-based release version; -PreleaseVersion= overrides for paper.yml publish.
version = providers.gradleProperty("releaseVersion").orElse("26.8.11.1").get()








subprojects {
    group = rootProject.group
    version = rootProject.version

    apply(plugin = "java")
    apply(plugin = "java-library")
    apply(plugin = "checkstyle")
    apply(plugin = "pmd")
    apply(plugin = "com.github.spotbugs")
    apply(plugin = "com.diffplug.spotless")
    apply(plugin = "net.ltgt.errorprone")

    val moduleName = name
    val javaVersion = 25
    configure<JavaPluginExtension> {
        toolchain.languageVersion.set(JavaLanguageVersion.of(javaVersion))
    }
    tasks.withType<JavaCompile>().configureEach {
        options.release.set(javaVersion)
    }

    if (moduleName == "api") {
        apply(plugin = "maven-publish")
        configure<JavaPluginExtension> {
            withSourcesJar()
            withJavadocJar()
        }
        tasks.withType<Javadoc>().configureEach {
            isFailOnError = false
        }
        configure<PublishingExtension> {
            publications {
                create<MavenPublication>("maven") {
                    from(components["java"])
                    artifactId = "modularjobs-$moduleName"
                    pom {
                        name.set("ModularJobs ${moduleName.replaceFirstChar(Char::uppercase)}")
                        description.set("Public Java contracts for the ModularJobs Paper plugin.")
                        url.set("https://github.com/aincraft-org/modularjobs")
                        licenses {
                            license {
                                name.set("MIT License")
                                url.set("https://opensource.org/licenses/MIT")
                                distribution.set("repo")
                            }
                        }
                        developers {
                            developer {
                                id.set("aincraft")
                                name.set("ModularJobs contributors")
                                url.set("https://github.com/aincraft-org")
                            }
                        }
                        scm {
                            connection.set("scm:git:git://github.com/aincraft-org/modularjobs.git")
                            developerConnection.set("scm:git:ssh://git@github.com/aincraft-org/modularjobs.git")
                            url.set("https://github.com/aincraft-org/modularjobs")
                        }
                    }
                }
            }
            repositories {
                maven {
                    name = "localBuildRepo"
                    url = rootProject.layout.buildDirectory.dir("maven-repo").get().asFile.toURI()
                }
                maven {
                    name = "GitHubPackages"
                    url = uri("https://maven.pkg.github.com/aincraft-org/modularjobs")
                    credentials {
                        username = System.getenv("GITHUB_ACTOR") ?: ""
                        password = System.getenv("GITHUB_TOKEN") ?: ""
                    }
                }
            }
        }


    }

    val qualityConfig = rootProject.layout.projectDirectory.dir("config")

    dependencies {
        add("errorprone", rootProject.libs.errorprone.core)
    }

    // --- Error Prone (compile-time; errors fail the build) ---
    tasks.withType<JavaCompile>().configureEach {
        options.errorprone {
            disableWarningsInGeneratedCode.set(true)
        }
    }

    // --- Checkstyle (Google Checks, fail-closed) ---
    configure<CheckstyleExtension> {
        toolVersion = rootProject.libs.versions.checkstyle.get()
        config = resources.text.fromUri(
            "https://raw.githubusercontent.com/checkstyle/checkstyle/checkstyle-13.11.0/src/main/resources/google_checks.xml"
        )
        isIgnoreFailures = false
        maxWarnings = 0
        isShowViolations = true
    }
    tasks.withType<Checkstyle>().configureEach {
        reports {
            xml.required.set(true)
            html.required.set(true)
        }
    }

    // --- PMD ---
    configure<PmdExtension> {
        toolVersion = rootProject.libs.versions.pmd.get()
        isConsoleOutput = true
        ruleSetFiles = files(qualityConfig.file("pmd/ruleset.xml"))
        ruleSets = emptyList() // use only our ruleset
        isIgnoreFailures = false
        threads = Runtime.getRuntime().availableProcessors().coerceAtMost(4)
    }
    tasks.withType<Pmd>().configureEach {
        reports {
            xml.required.set(true)
            html.required.set(true)
        }
    }

    // --- SpotBugs ---
    configure<SpotBugsExtension> {
        toolVersion.set(rootProject.libs.versions.spotbugs.tool)
        ignoreFailures.set(false)
        showStackTraces.set(true)
        showProgress.set(false)
        effort.set(Effort.MORE)
        reportLevel.set(Confidence.MEDIUM)
        excludeFilter.set(qualityConfig.file("spotbugs/exclude.xml"))
    }
    tasks.withType<SpotBugsTask>().configureEach {
        reports.create("html") {
            required.set(true)
        }
        reports.create("xml") {
            required.set(true)
        }
    }

    configure<com.diffplug.gradle.spotless.SpotlessExtension> {
        java {
            googleJavaFormat("1.36.1")
            target("src/**/*.java")
        }
    }

    tasks.named("check") {
        dependsOn(tasks.withType<Checkstyle>())
        dependsOn(tasks.withType<Pmd>())
        dependsOn(tasks.withType<SpotBugsTask>())
        dependsOn(tasks.named("spotlessCheck"))
    }
}
