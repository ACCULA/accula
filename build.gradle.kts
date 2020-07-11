import org.gradle.api.tasks.testing.logging.TestLogEvent.FAILED
import org.gradle.api.tasks.testing.logging.TestLogEvent.PASSED
import org.gradle.api.tasks.testing.logging.TestLogEvent.SKIPPED

plugins {
    java
    jacoco
    pmd
    id("net.bytebuddy.byte-buddy-gradle-plugin") version "1.10.11"
    id("org.springframework.boot") version "2.3.1.RELEASE" apply false
    id("io.spring.dependency-management") version "1.0.9.RELEASE" apply false
}

jacoco {
    toolVersion = "0.8.5"
}

allprojects {
    group = "org.accula"
}

configure(subprojects) {
    apply(plugin = "java")
    apply(plugin = "net.bytebuddy.byte-buddy-gradle-plugin")

    val byteBuddyPlugin: Configuration by configurations.creating

    repositories {
        jcenter()
    }

    dependencies {
        implementation("org.jetbrains:annotations:19.0.0")

        implementation("org.slf4j:slf4j-api:2.0.0-alpha1")
        implementation("org.slf4j:slf4j-log4j12:2.0.0-alpha1")

        compileOnly("io.projectreactor:reactor-tools:3.3.5.RELEASE")
        byteBuddyPlugin(group = "io.projectreactor", name = "reactor-tools", classifier = "original")

        val lombok = "org.projectlombok:lombok:1.18.12"
        compileOnly(lombok)
        annotationProcessor(lombok)
        testCompileOnly(lombok)
        testAnnotationProcessor(lombok)
    }

    configure<JavaPluginConvention> {
        sourceCompatibility = JavaVersion.VERSION_14
    }

    tasks {
        test {
            useJUnitPlatform()

            testLogging {
                events(PASSED, SKIPPED, FAILED)
            }
        }
    }

    tasks.withType<JavaCompile> {
        options.compilerArgs.add("--enable-preview")
    }

    tasks.withType<Test> {
        jvmArgs("--enable-preview")
    }

    tasks.withType<JavaExec> {
        jvmArgs("--enable-preview")
    }

    byteBuddy {
        transformation(closureOf<net.bytebuddy.build.gradle.Transformation> {
            plugin = "reactor.tools.agent.ReactorDebugByteBuddyPlugin"
            setClassPath(byteBuddyPlugin)
        })
    }
}

fun needsJaCoCo(project: Project) = !sequenceOf(":github").any { project == project(it) }

configure(subprojects.filter(::needsJaCoCo)) {
    apply(plugin = "jacoco")

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
            finalizedBy(jacocoTestReport)
        }
    }
}
