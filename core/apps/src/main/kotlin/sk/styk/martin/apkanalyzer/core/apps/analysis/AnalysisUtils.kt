package sk.styk.martin.apkanalyzer.core.apps.analysis

import java.io.File

fun computeApkSize(sourceDir: String?): Long = sourceDir?.let { File(it).length() } ?: 0L

internal fun createSimpleName(name: String): String = name.substringAfterLast('.', name)
    .split('_')
    .joinToString(" ") { it.lowercase().replaceFirstChar(Char::uppercaseChar) }
