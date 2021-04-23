plugins {
    id("org.siouan.frontend-jdk11") version "5.0.1"
}

frontend {
    nodeVersion.set("14.16.1")
    yarnEnabled.set(true)
    yarnVersion.set("1.22.5")
    cleanScript.set("run clean")
    assembleScript.set("run build")
    checkScript.set("run check")
}
