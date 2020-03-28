plugins {
    id("org.springframework.boot") version "2.2.6.RELEASE"
    id("io.spring.dependency-management") version "1.0.9.RELEASE"
}

version = "1.0-SNAPSHOT"

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-webflux:2.2.6.RELEASE")
    implementation("org.springframework.boot:spring-boot-starter-security:2.2.6.RELEASE")
    implementation("org.jetbrains:annotations:19.0.0")

    val lombok = "org.projectlombok:lombok:1.18.12"
    compileOnly(lombok)
    annotationProcessor(lombok)

    testImplementation("org.springframework.boot:spring-boot-starter-test:2.2.6.RELEASE") {
        exclude(group = "org.junit.vintage", module = "junit-vintage-engine")
    }
    testImplementation("org.springframework.security:spring-security-test:5.3.0.RELEASE")

    testCompileOnly(lombok)
    testAnnotationProcessor(lombok)
}
