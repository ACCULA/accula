plugins {
    antlr
    java
}

group = "org.accula.research"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven(url = "https://dl.bintray.com/vorpal-research/kotlin-maven/")
}

configure<JavaPluginConvention> {
    sourceCompatibility = JavaVersion.VERSION_14
}

dependencies {
    antlr("org.antlr:antlr4:4.8-1")
    compileOnly("org.antlr:antlr4-runtime:4.8-1")

    implementation("com.suhininalex:suffixtree:1.0.2")

    implementation("org.jetbrains:annotations:19.0.0")

    implementation(fileTree("dir" to "libs", "include" to  "*.jar"))

    val lombok = "org.projectlombok:lombok:1.18.12"
    compileOnly(lombok)
    annotationProcessor(lombok)
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

