plugins {
    id("org.springframework.boot") version "2.3.1.RELEASE"
    id("io.spring.dependency-management") version "1.0.9.RELEASE"
    id("net.bytebuddy.byte-buddy-gradle-plugin") version "1.10.11"
    antlr
}

repositories {
    maven(url = "https://dl.bintray.com/vorpal-research/kotlin-maven/")
    maven(url =  "https://dl.bintray.com/accula/clone-detector")
}

version = "1.0-SNAPSHOT"

val byteBuddyPlugin: Configuration by configurations.creating
val antlrVersion = "4.8-1"

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-webflux")
    testImplementation("org.springframework.boot:spring-boot-starter-test") {
        exclude(group = "org.junit.vintage", module = "junit-vintage-engine")
    }
    implementation("org.springframework.boot:spring-boot-starter-security")
    testImplementation("org.springframework.security:spring-security-test")

    implementation("io.projectreactor.addons:reactor-extra")
    compileOnly("io.projectreactor:reactor-tools:3.3.5.RELEASE")
    byteBuddyPlugin(group = "io.projectreactor", name = "reactor-tools", classifier = "original")

    testImplementation("io.projectreactor:reactor-test")

    implementation("org.springframework.boot:spring-boot-starter-actuator")

    implementation("org.springframework.boot:spring-boot-starter-oauth2-client")
    implementation("org.springframework.security:spring-security-oauth2-client")

    implementation("com.auth0:java-jwt:3.10.2")

    implementation("org.springframework.boot:spring-boot-starter-data-r2dbc")
    implementation("io.r2dbc:r2dbc-postgresql")
    implementation("io.r2dbc:r2dbc-pool")
    implementation("io.r2dbc:r2dbc-spi")

    implementation("org.slf4j:slf4j-api:2.0.0-alpha1")
    implementation("org.slf4j:slf4j-log4j12:2.0.0-alpha1")

    implementation("org.postgresql:postgresql")
    implementation("org.springframework:spring-jdbc")
    implementation("org.flywaydb:flyway-core")

    annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")

    antlr("org.antlr:antlr4:$antlrVersion")
    compileOnly("org.antlr:antlr4-runtime:$antlrVersion")

    implementation("com.suhininalex:suffixtree:1.0.2")
    implementation("org.accula:clone-detector:1.0.1")
}

byteBuddy {
    transformation(closureOf<net.bytebuddy.build.gradle.Transformation> {
        plugin = "reactor.tools.agent.ReactorDebugByteBuddyPlugin"
        setClassPath(byteBuddyPlugin)
    })
}

tasks.generateGrammarSource {
    maxHeapSize = "64m"
    arguments = arguments + listOf("-package", "generated")
    outputDirectory = File("src/main/java/generated")
}
