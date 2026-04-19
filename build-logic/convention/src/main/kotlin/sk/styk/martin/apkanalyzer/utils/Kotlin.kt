package sk.styk.martin.apkanalyzer.utils

import org.gradle.api.Project
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.dsl.KotlinAndroidProjectExtension
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile


internal fun Project.configureKotlin() {
    extensions.configure(KotlinAndroidProjectExtension::class.java) {
        jvmToolchain(21)
    }

    tasks.withType<KotlinCompile>().configureEach {
        compilerOptions {
            freeCompilerArgs.addAll(
                "-opt-in=kotlinx.coroutines.ExperimentalCoroutinesApi",
                "-opt-in=kotlinx.coroutines.FlowPreview",
                "-Xannotation-default-target=param-property",
            )
        }

    }
}
