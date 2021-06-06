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
    repositories {
        mavenCentral()
    }

    apply(plugin = "java")
    apply(plugin = "jacoco")

    jacoco {
        toolVersion = "0.8.7"
    }

    dependencies {
        compileOnly("org.jetbrains:annotations:20.1.0")

        val lombok = "org.projectlombok:lombok:1.18.20"
        compileOnly(lombok)
        annotationProcessor(lombok)
        testCompileOnly(lombok)
        testAnnotationProcessor(lombok)
    }

    configure<JavaPluginConvention> {
        sourceCompatibility = JavaVersion.VERSION_16
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
                    report.destination = file("$buildDir/reports/jacoco/coverage.${report.name}")
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

    tasks.withType<JavaCompile> {
        options.compilerArgs.addAll(listOf(
                "-Xlint:deprecation",
                "-Xlint:unchecked",
        ))
    }
    tasks.withType<Test> {
        val testSingleLineRangeCacheSize: String by project
        systemProperty("org.accula.api.code.lines.LineRange.Single.Cache.size", testSingleLineRangeCacheSize)
    }
}
