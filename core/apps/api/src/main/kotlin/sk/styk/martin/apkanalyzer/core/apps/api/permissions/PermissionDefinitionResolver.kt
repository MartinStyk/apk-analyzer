package sk.styk.martin.apkanalyzer.core.apps.api.permissions

interface PermissionDefinitionResolver {
    fun resolve(name: String): PermissionDetails?
}
