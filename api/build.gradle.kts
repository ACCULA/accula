plugins {
    id("org.springframework.boot") version "2.3.0.RC1"
    id("io.spring.dependency-management") version "1.0.9.RELEASE"
}

version = "1.0-SNAPSHOT"

repositories {
    maven(url = "https://repo.spring.io/milestone")
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-webflux")
    testImplementation("org.springframework.boot:spring-boot-starter-test") {
        exclude(group = "org.junit.vintage", module = "junit-vintage-engine")
    }
    implementation("org.springframework.boot:spring-boot-starter-security")
    testImplementation("org.springframework.security:spring-security-test")

    implementation("org.springframework.boot:spring-boot-starter-actuator")

    implementation("org.springframework.boot:spring-boot-starter-oauth2-client")
    implementation("org.springframework.security:spring-security-oauth2-client")

    implementation("com.auth0:java-jwt:3.10.2")

    implementation("io.projectreactor:reactor-tools")

    implementation("org.springframework.boot:spring-boot-starter-data-r2dbc")
    implementation("io.r2dbc:r2dbc-postgresql:0.8.2.RELEASE")
    implementation("io.r2dbc:r2dbc-pool:0.8.2.RELEASE")
    implementation("io.r2dbc:r2dbc-spi:0.8.1.RELEASE")

    annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")
}
