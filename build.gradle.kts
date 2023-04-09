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
        toolVersion = "0.8.9"
    }

    dependencies {
        compileOnly("org.jetbrains:annotations:24.0.1")

        val lombok = "org.projectlombok:lombok:1.18.26"
        compileOnly(lombok)
        annotationProcessor(lombok)
        testCompileOnly(lombok)
        testAnnotationProcessor(lombok)
    }

    java {
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(19))
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

            dependsOn(test)
        }

        test {
            useJUnitPlatform()

            testLogging {
                events(PASSED, SKIPPED, FAILED)
            }

            val testSingleLineRangeCacheSize: String by project
            systemProperty("org.accula.api.code.lines.LineRange.Single.Cache.size", testSingleLineRangeCacheSize)

            jvmArgs("--enable-preview")
        }

        compileJava {
            options.compilerArgs.addAll(listOf(
                "-Xlint:deprecation",
                "-Xlint:unchecked",
                "-Xlint:preview",
                "--enable-preview",
            ))
        }

        compileTestJava {
            options.compilerArgs.addAll(listOf(
                "-Xlint:preview",
                "--enable-preview",
            ))
        }

        withType<JavaExec> {
            jvmArgs("--enable-preview")
        }
    }
}
