import com.github.spotbugs.snom.Confidence
import com.github.spotbugs.snom.Effort
import com.github.spotbugs.snom.SpotBugsExtension
import com.github.spotbugs.snom.SpotBugsTask
import net.ltgt.gradle.errorprone.errorprone

plugins {
    alias(libs.plugins.errorprone) apply false
    alias(libs.plugins.spotbugs) apply false
}

group = "org.aincraft"
version = "1.2.0"

subprojects {
    apply(plugin = "java")
    apply(plugin = "java-library")
    apply(plugin = "checkstyle")
    apply(plugin = "pmd")
    apply(plugin = "com.github.spotbugs")
    apply(plugin = "net.ltgt.errorprone")

    configure<JavaPluginExtension> {
        // Paper 26.2 / MockBukkit 26.2 require Java 25
        toolchain.languageVersion.set(JavaLanguageVersion.of(25))
    }

    // Quality tools run on `check` and always write reports.
    // Default: report-only (pre-existing findings). Enforce with -Pquality.fail=true
    val qualityFail = providers.gradleProperty("quality.fail")
        .map(String::toBoolean)
        .orElse(false)
    val qualityIgnoreFailures = qualityFail.map { !it }

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

    // --- Checkstyle ---
    // configDirectory supplies ${config_loc} for SuppressionFilter in checkstyle.xml
    configure<CheckstyleExtension> {
        toolVersion = rootProject.libs.versions.checkstyle.get()
        configDirectory.set(qualityConfig.dir("checkstyle"))
        configFile = qualityConfig.file("checkstyle/checkstyle.xml").asFile
        isIgnoreFailures = qualityIgnoreFailures.get()
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
        isIgnoreFailures = qualityIgnoreFailures.get()
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
        ignoreFailures.set(qualityIgnoreFailures)
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
}
