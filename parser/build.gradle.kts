plugins {
    antlr
}

version = "1.0-SNAPSHOT"

dependencies {
    antlr("org.antlr:antlr4:4.8-1")
    compileOnly("org.antlr:antlr4-runtime:4.8-1")
    implementation("io.projectreactor:reactor-core:3.3.5.RELEASE")
    implementation("org.slf4j:slf4j-log4j12:1.7.30")

    testImplementation("org.junit.jupiter:junit-jupiter-api:5.6.2")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.6.2")
    testImplementation("io.projectreactor:reactor-test:3.3.5.RELEASE")
}

tasks.generateGrammarSource {
    maxHeapSize = "64m"
    arguments = arguments + listOf("-package", "generated.org.accula.parser")
    outputDirectory = File("src/main/java/generated")
}
