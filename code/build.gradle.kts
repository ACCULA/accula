plugins {
    id("org.springframework.boot") version "2.3.0.RELEASE"
    id("io.spring.dependency-management") version "1.0.9.RELEASE"
}

version = "1.0-SNAPSHOT"

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-webflux")
    implementation("org.eclipse.jgit:org.eclipse.jgit:5.7.0.202003110725-r")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")
}
