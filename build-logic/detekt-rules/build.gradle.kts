import dev.detekt.gradle.extensions.FailOnSeverity

plugins {
    `embedded-kotlin`
    `java-library`
    alias(libs.plugins.detekt)
}

group = "sk.styk.martin.apkanalyzer.buildlogic"

kotlin {
    jvmToolchain(25)
}

dependencies {
    compileOnly(libs.detekt.api)

    testImplementation(libs.detekt.api)
    testImplementation(libs.detekt.test)
    testImplementation(libs.junit)
    testImplementation(libs.kotest.assertions.core)

    detektPlugins(files(tasks.jar))
}

tasks.test {
    useJUnit()
}

detekt {
    buildUponDefaultConfig.set(true)
    config.setFrom(layout.projectDirectory.file("../../.detekt/detekt.yml"))
    basePath.set(layout.projectDirectory.dir("../.."))
    parallel.set(true)
    failOnSeverity.set(FailOnSeverity.Warning)
}
