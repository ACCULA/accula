import org.gradle.api.tasks.testing.logging.TestLogEvent.FAILED
import org.gradle.api.tasks.testing.logging.TestLogEvent.PASSED
import org.gradle.api.tasks.testing.logging.TestLogEvent.SKIPPED

plugins {
    java
    jacoco
}

allprojects {
    group = "org.accula"
}

configure(subprojects.filterNot(project(":web")::equals)) {
    apply(plugin = "java")
    apply(plugin = "jacoco")

    repositories {
        jcenter()
    }

    configure<JavaPluginConvention> {
        sourceCompatibility = JavaVersion.VERSION_14
    }

    tasks {
        jacocoTestReport {
            executionData(fileTree(project.rootDir.absolutePath).include("**/build/jacoco/*.exec"))

            subprojects.forEach {
                sourceSets(it.sourceSets["main"])
            }

            reports {
                sourceDirectories.from(files(sourceSets["main"].allSource.srcDirs))
                classDirectories.from(files(sourceSets["main"].output))

                csv.isEnabled = false

                listOf(xml, html).forEach { report ->
                    report.isEnabled = true
                    report.destination = file("$buildDir/reports/jacoco/report.${report.name}")
                }
            }
        }

        test {
            useJUnitPlatform()

            testLogging {
                events(PASSED, SKIPPED, FAILED)
            }

            finalizedBy(jacocoTestReport)
        }
    }
}

jacoco {
    toolVersion = "0.8.5"
    reportsDir = file("$buildDir/customJacocoReportDir")
}
