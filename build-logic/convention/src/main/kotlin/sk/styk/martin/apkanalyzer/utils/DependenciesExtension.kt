package sk.styk.martin.apkanalyzer.utils

import org.gradle.api.Action
import org.gradle.api.artifacts.Dependency
import org.gradle.api.artifacts.dsl.DependencyHandler
import org.gradle.api.provider.Provider

internal fun DependencyHandler.implementation(dependencyNotation: Any): Dependency? = add("implementation", dependencyNotation)

internal fun DependencyHandler.implementation(dependencyNotation: Provider<*>, configuration: Action<*>) = addProvider("implementation", dependencyNotation, configuration)

internal fun DependencyHandler.api(dependencyNotation: Any): Dependency? = add("api", dependencyNotation)

internal fun DependencyHandler.api(dependencyNotation: Provider<*>, configuration: Action<*>) = addProvider("api", dependencyNotation, configuration)

internal fun DependencyHandler.ksp(dependencyNotation: Any): Dependency? = add("ksp", dependencyNotation)
