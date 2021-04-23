plugins {
    id("org.siouan.frontend") version "3.0.2"
}

frontend {
    nodeVersion.set("14.16.1")
    yarnEnabled.set(true)
    yarnVersion.set("1.22.5")
    cleanScript.set("run clean")
    assembleScript.set("run build")
    checkScript.set("run check")
}
