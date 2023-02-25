plugins {
    id("org.springframework.boot") version "2.7.4"
    id("io.spring.dependency-management") version "1.0.14.RELEASE"
    kotlin("jvm") version "1.7.10"
}

repositories {
    mavenCentral()

    maven(url = "https://www.jetbrains.com/intellij-repository/releases")
    maven(url = "https://cache-redirector.jetbrains.com/intellij-dependencies")

    maven {
        url = uri("https://maven.pkg.github.com/accula/suffix-tree")

        credentials {
            username = gprCredentialWith(propertyNamed = "gpr.user", orEnvVarNamed = "GPR_USERNAME")
            password = gprCredentialWith(propertyNamed = "gpr.key", orEnvVarNamed = "GPR_TOKEN")
        }
    }
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

    implementation("com.auth0:java-jwt:4.0.0")

    implementation("org.springframework.boot:spring-boot-starter-data-r2dbc")
    implementation("org.postgresql:r2dbc-postgresql")
    implementation("io.r2dbc:r2dbc-pool")

    implementation("org.slf4j:slf4j-api:2.0.3")
    implementation("org.slf4j:slf4j-log4j12:2.0.3")

    implementation("org.postgresql:postgresql")
    implementation("org.springframework:spring-jdbc")
    implementation("org.flywaydb:flyway-core")

    annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")

    implementation("org.codeclone:suffix-tree:1.0.0")
    implementation("com.jetbrains.intellij.java:java-psi-impl:211.7628.21")
    implementation(kotlin("compiler-embeddable"))
    implementation("com.google.guava:guava:31.1-jre")
    implementation("it.unimi.dsi:fastutil:8.5.9")
    implementation("info.debatty:java-string-similarity:2.0.0")
    implementation("commons-codec:commons-codec:1.15")

    implementation(platform("org.testcontainers:testcontainers-bom:1.17.5"))
    testImplementation("org.testcontainers:testcontainers")
    testImplementation("org.testcontainers:junit-jupiter")
    testImplementation("org.testcontainers:postgresql")
    testImplementation("org.testcontainers:r2dbc")
}

tasks {
    compileKotlin {
        enabled = false
    }
}

fun gprCredentialWith(propertyNamed: String, orEnvVarNamed: String) = project.findProperty(propertyNamed) as String?
    ?: System.getenv(orEnvVarNamed)
    ?: throw IllegalStateException("Either project property '$propertyNamed' or environment variable '$orEnvVarNamed' MUST be present for successful GPR authentication")
