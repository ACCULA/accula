plugins {
    id("org.springframework.boot") version "2.3.2.RELEASE"
    antlr
    java
}

group = "org.accula.research"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven(url = "https://dl.bintray.com/vorpal-research/kotlin-maven/")
    maven(url = "https://dl.bintray.com/accula/clone-detector")
}

configure<JavaPluginConvention> {
    sourceCompatibility = JavaVersion.VERSION_14
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-webflux:2.3.1.RELEASE")

    antlr("org.antlr:antlr4:4.8-1")
    compileOnly("org.antlr:antlr4-runtime:4.8-1")
    implementation("com.github.javaparser:javaparser-core:3.16.1")

    implementation("com.suhininalex:suffixtree:1.0.2")
    implementation("org.accula:clone-detector:1.0.1")

    implementation("org.jetbrains:annotations:19.0.0")
    val lombok = "org.projectlombok:lombok:1.18.12"
    compileOnly(lombok)
    annotationProcessor(lombok)

    implementation("ch.qos.logback:logback-classic:1.2.3")
}

tasks.generateGrammarSource {
    maxHeapSize = "64m"
    arguments = arguments + listOf("-package", "generated.org.accula.parser")
    outputDirectory = File("src/main/java/generated")
}

tasks.withType<JavaCompile> {
    options.compilerArgs.add("--enable-preview")
}
tasks.withType<Test> {
    jvmArgs("--enable-preview")
}
tasks.withType<JavaExec> {
    jvmArgs("--enable-preview")
}
