package sk.styk.martin.apkanalyzer.core.appexport.column

import sk.styk.martin.apkanalyzer.core.apps.devicefeatures.Feature
import sk.styk.martin.apkanalyzer.core.apps.packaging.InstalledSplitApk
import sk.styk.martin.apkanalyzer.core.apps.packaging.NativeLibraryFile
import sk.styk.martin.apkanalyzer.core.apps.permissions.Permission

internal fun InstalledSplitApk.toInventoryValue(): InventoryValue = record(
    "file_name" to text(fileName),
    "file_path" to text(filePath),
    "size_bytes" to number(size.bytes),
    "kind" to text(kind.token),
    "qualifier" to text(qualifier),
)

internal fun NativeLibraryFile.toInventoryValue(): InventoryValue = record(
    "name" to text(name),
    "abi" to text(abi),
    "size_bytes" to number(size.bytes),
    "containing_apk" to text(containingApkFileName),
)

internal fun Feature.toInventoryValue(): InventoryValue = when (this) {
    is Feature.Hardware -> record(
        "kind" to text("hardware"),
        "name" to text(name),
        "version" to number(version.toLong()),
        "required" to flag(isRequired),
    )

    is Feature.OpenGlEs -> record(
        "kind" to text("opengl_es"),
        "version" to number(reqGlEsVersion.toLong()),
        "version_name" to text(versionName),
        "required" to flag(isRequired),
    )
}

internal fun Permission.toInventoryValue(): InventoryValue = record(
    "name" to text(name),
    "details" to (
        details?.let {
            record(
                "group_name" to text(it.groupName),
                "protection_level" to text(it.protectionLevel.token),
                "protection_flags" to InventoryValue.TextList(it.protectionFlags.map { flag -> flag.token }.sorted()),
                "description" to text(it.description),
                "declaring_package" to text(it.declaringPackage.value),
            )
        } ?: InventoryValue.Missing
        ),
)
