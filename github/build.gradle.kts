plugins {
    id("io.spring.dependency-management")
    id("org.springframework.boot")
}

version = "1.0-SNAPSHOT"

dependencies {
    implementation("org.springframework:spring-webflux")
    implementation("com.fasterxml.jackson.core:jackson-annotations")

    annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")
}

tasks.getByName("bootJar") {
    enabled = false
}

tasks.getByName("jar") {
    enabled = true
}
