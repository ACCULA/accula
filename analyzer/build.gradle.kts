version = "1.0-SNAPSHOT"

dependencies {
    implementation(project(":parser"))
    implementation("io.projectreactor:reactor-core:3.3.5.RELEASE")
    implementation("org.slf4j:slf4j-log4j12:1.7.30")

    testImplementation("org.junit.jupiter:junit-jupiter-api:5.6.2")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.6.2")
    testImplementation("io.projectreactor:reactor-test:3.3.5.RELEASE")
}

val fatJar = task("fatJar", type = Jar::class) {
    base.archivesBaseName = "${project.name}-fat"
    manifest {
        attributes["Main-Class"] = "org.accula.analyzer.LocalRunner"
    }
    from(configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) })
    {
        exclude("META-INF/*.SF")
        exclude("META-INF/*.DSA")
        exclude("META-INF/*.RSA")
    }
    with(tasks.jar.get() as CopySpec)
}
