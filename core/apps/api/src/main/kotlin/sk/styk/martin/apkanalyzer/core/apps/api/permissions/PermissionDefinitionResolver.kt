package sk.styk.martin.apkanalyzer.core.apps.permissions

interface PermissionDefinitionResolver {
    fun resolve(name: String): PermissionDetails?
}
