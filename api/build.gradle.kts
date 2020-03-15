plugins {
    java

    id("org.springframework.boot") version "2.2.5.RELEASE"
    id("io.spring.dependency-management") version "1.0.9.RELEASE"
}

version = "1.0-SNAPSHOT"

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-webflux:2.2.5.RELEASE")
    implementation("org.jetbrains:annotations:19.0.0")

    val lombok = "org.projectlombok:lombok:1.18.12"
    compileOnly(lombok)
    annotationProcessor(lombok)

    testImplementation("org.springframework.boot:spring-boot-starter-test:2.2.5.RELEASE") {
        exclude(module = "junit")
    }
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.6.0")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.6.0")

    testCompileOnly(lombok)
    testAnnotationProcessor(lombok)
}

configure<JavaPluginConvention> {
    sourceCompatibility = JavaVersion.VERSION_13
}
