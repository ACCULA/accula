plugins {
    id("org.springframework.boot") version "2.3.0.RELEASE"
    id("io.spring.dependency-management") version "1.0.9.RELEASE"
}

version = "1.0-SNAPSHOT"

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-webflux")
    testImplementation("org.springframework.boot:spring-boot-starter-test") {
        exclude(group = "org.junit.vintage", module = "junit-vintage-engine")
    }
    implementation("org.springframework.boot:spring-boot-starter-security")
    testImplementation("org.springframework.security:spring-security-test")

    implementation("io.projectreactor:reactor-tools:3.3.6.RELEASE")
    testImplementation("io.projectreactor:reactor-test:3.3.6.RELEASE")

    implementation("org.springframework.boot:spring-boot-starter-actuator")

    implementation("org.springframework.boot:spring-boot-starter-oauth2-client")
    implementation("org.springframework.security:spring-security-oauth2-client")

    implementation("com.auth0:java-jwt:3.10.2")

    implementation("org.springframework.boot:spring-boot-starter-data-r2dbc")
    implementation("io.r2dbc:r2dbc-postgresql:0.8.2.RELEASE")
    implementation("io.r2dbc:r2dbc-pool:0.8.2.RELEASE")
    implementation("io.r2dbc:r2dbc-spi:0.8.1.RELEASE")

    implementation("org.slf4j:slf4j-api:2.0.0-alpha1")
    implementation("org.slf4j:slf4j-log4j12:2.0.0-alpha1")

    implementation("org.postgresql:postgresql")
    implementation("org.springframework:spring-jdbc")
    implementation("org.flywaydb:flyway-core")

    implementation("org.eclipse.jgit:org.eclipse.jgit:5.7.0.202003110725-r") {
        exclude(group = "org.slf4j", module = "slf4j-api")
    }

    annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")
}
