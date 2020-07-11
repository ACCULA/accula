plugins {
    id("org.springframework.boot")
    id("io.spring.dependency-management")
}

version = "1.0-SNAPSHOT"

dependencies {
    implementation(project(":github"))

    implementation("org.springframework.boot:spring-boot-starter-webflux")
    testImplementation("org.springframework.boot:spring-boot-starter-test") {
        exclude(group = "org.junit.vintage", module = "junit-vintage-engine")
    }
    implementation("org.springframework.boot:spring-boot-starter-security")
    testImplementation("org.springframework.security:spring-security-test")

    testImplementation("io.projectreactor:reactor-test")

    implementation("org.springframework.boot:spring-boot-starter-actuator")

    implementation("org.springframework.boot:spring-boot-starter-oauth2-client")
    implementation("org.springframework.security:spring-security-oauth2-client")

    implementation("com.auth0:java-jwt:3.10.2")

    implementation("org.springframework.boot:spring-boot-starter-data-r2dbc")
    implementation("io.r2dbc:r2dbc-postgresql")
    implementation("io.r2dbc:r2dbc-pool")
    implementation("io.r2dbc:r2dbc-spi")

    implementation("org.postgresql:postgresql")
    implementation("org.springframework:spring-jdbc")
    implementation("org.flywaydb:flyway-core")

    implementation("org.eclipse.jgit:org.eclipse.jgit:5.7.0.202003110725-r") {
        exclude(group = "org.slf4j", module = "slf4j-api")
    }

    annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")
}
