package sk.styk.martin.apkanalyzer.utils

import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.dsl.LibraryExtension
import com.diffplug.gradle.spotless.SpotlessExtension
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalog
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.kotlin.dsl.getByType

val Project.androidLibrary: LibraryExtension
    get() = extensions.getByType(LibraryExtension::class.java)

val Project.androidApplication: ApplicationExtension
    get() = extensions.getByType(ApplicationExtension::class.java)

val Project.spotless: SpotlessExtension
    get() = extensions.getByType(SpotlessExtension::class.java)

val Project.libs
    get(): VersionCatalog = extensions.getByType<VersionCatalogsExtension>().named("libs")

fun Project.stringProperty(name: String): String? = findProperty(name) as? String
