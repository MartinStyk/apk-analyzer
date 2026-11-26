package sk.styk.martin.apkanalyzer.core.apppermissions.api.detail

import sk.styk.martin.apkanalyzer.core.apps.api.permissions.Permission

interface PermissionDescriptionProvider {
    fun describe(permission: Permission): String?
}
