plugins {
    antlr
    id("org.springframework.boot") version "2.2.6.RELEASE"
    id("io.spring.dependency-management") version "1.0.9.RELEASE"
}

version = "1.0-SNAPSHOT"

tasks.bootJar {
    enabled = false
}

tasks.jar {
    enabled = true
}

dependencies {
    antlr("org.antlr:antlr4:4.8-1")
    compileOnly("org.antlr:antlr4-runtime:4.8-1")

    implementation("org.springframework.boot:spring-boot-starter-webflux:2.2.6.RELEASE")
    implementation("org.jetbrains:annotations:19.0.0")

    implementation("org.apache.commons:commons-text:1.8")

    val lombok = "org.projectlombok:lombok:1.18.12"
    compileOnly(lombok)
    annotationProcessor(lombok)

    testImplementation("org.springframework.boot:spring-boot-starter-test:2.2.6.RELEASE") {
        exclude(group = "org.junit.vintage", module = "junit-vintage-engine")
    }

    testCompileOnly(lombok)
    testAnnotationProcessor(lombok)
}

tasks.generateGrammarSource {
    maxHeapSize = "64m"
    arguments = arguments + listOf("-package", "org.accula.parser", "-visitor")
    outputDirectory = File("src/main/java")
}
