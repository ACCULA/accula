plugins {
    id("org.springframework.boot") version "2.4.4"
    id("io.spring.dependency-management") version "1.0.11.RELEASE"
}

repositories {
    maven(url = "https://dl.bintray.com/vorpal-research/kotlin-maven")
    maven(url = "https://www.jetbrains.com/intellij-repository/releases")
    maven(url = "https://jetbrains.bintray.com/intellij-third-party-dependencies")
}

version = "1.0-SNAPSHOT"

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-webflux")
    testImplementation("org.springframework.boot:spring-boot-starter-test") {
        exclude(group = "org.junit.vintage", module = "junit-vintage-engine")
    }
    implementation("org.springframework.boot:spring-boot-starter-security")
    testImplementation("org.springframework.security:spring-security-test")

    implementation("io.projectreactor.addons:reactor-extra")
    implementation("io.projectreactor:reactor-tools")

    testImplementation("io.projectreactor:reactor-test")

    implementation("org.springframework.boot:spring-boot-starter-actuator")

    implementation("org.springframework.boot:spring-boot-starter-oauth2-client")
    implementation("org.springframework.security:spring-security-oauth2-client")

    implementation("com.auth0:java-jwt:3.15.0")

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

    implementation("com.suhininalex:suffixtree:1.0.2")
    implementation("com.jetbrains.intellij.java:java-psi-impl:203.7148.57")
    implementation("com.google.guava:guava:30.1.1-jre")
    implementation("it.unimi.dsi:fastutil:8.5.2")
    implementation("info.debatty:java-string-similarity:2.0.0")
}
