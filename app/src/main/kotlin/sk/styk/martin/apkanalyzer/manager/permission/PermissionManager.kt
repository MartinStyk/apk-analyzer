package sk.styk.martin.apkanalyzer.manager.permission

interface PermissionManager {
    interface PermissionsCallback {
        fun onPermissionsGranted(grantedPermissions: List<String>)

        fun onPermissionsDenied(deniedPermissions: List<String>)
    }

    interface PermissionCallback {
        fun onPermissionGranted(permission: String)

        fun onPermissionDenied(permission: String)
    }

    fun hasPermissionGranted(permission: String): Boolean

    fun shouldShowRationaleForPermission(permission: String): Boolean

    fun requestPermission(permission: String, callback: PermissionCallback)

    fun requestPermissions(permissions: Array<String>, callback: PermissionsCallback)

    fun onRequestPermissionsResult(permissions: Array<String>, grantResults: IntArray)
}
