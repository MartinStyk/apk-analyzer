package sk.styk.martin.apkanalyzer.core.apps.analysis

import sk.styk.martin.apkanalyzer.core.common.model.AppSize
import sk.styk.martin.apkanalyzer.core.common.model.bytes
import java.io.File

fun computeApkSize(sourceDir: String?): AppSize = (sourceDir?.let { File(it).length() } ?: 0L).bytes

internal fun createSimpleName(name: String): String = name.substringAfterLast('.', name)
    .split('_')
    .joinToString(" ") { it.lowercase().replaceFirstChar(Char::uppercaseChar) }
