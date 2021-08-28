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
        compileOnly("org.jetbrains:annotations:22.0.0")

        val lombok = "org.projectlombok:lombok:1.18.20"
        compileOnly(lombok)
        annotationProcessor(lombok)
        testCompileOnly(lombok)
        testAnnotationProcessor(lombok)
    }

    java {
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(16))
        }
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

                csv.required.set(false)
                xml.required.set(true)
                html.required.set(true)
            }
        }

        test {
            useJUnitPlatform()

            testLogging {
                events(PASSED, SKIPPED, FAILED)
            }

            val testSingleLineRangeCacheSize: String by project
            systemProperty("org.accula.api.code.lines.LineRange.Single.Cache.size", testSingleLineRangeCacheSize)

            finalizedBy(jacocoTestReport)
        }

        compileJava {
            options.compilerArgs.addAll(
                listOf(
                    "-Xlint:deprecation",
                    "-Xlint:unchecked",
                )
            )
        }
    }
}
