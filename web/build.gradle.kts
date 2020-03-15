plugins {
    id("org.siouan.frontend") version "1.3.1"
}

frontend {
    nodeVersion.set("13.10.1")
    yarnEnabled.set(true)
    yarnVersion.set("1.22.4")
    cleanScript.set("run clean")
    assembleScript.set("run build")
    checkScript.set("run check")
}
