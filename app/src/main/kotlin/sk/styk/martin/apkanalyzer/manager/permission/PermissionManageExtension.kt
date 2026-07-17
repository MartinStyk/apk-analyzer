package sk.styk.martin.apkanalyzer.manager.permission

import android.Manifest

fun PermissionManager.withNotificationPermission(showNotification: () -> Unit) {
    if (!needsNotificationPermission() || hasPermissionGranted(Manifest.permission.POST_NOTIFICATIONS)) {
        showNotification()
    } else {
        requestPermission(
            Manifest.permission.POST_NOTIFICATIONS,
            object : PermissionManager.PermissionCallback {
                override fun onPermissionDenied(permission: String) {
                }

                override fun onPermissionGranted(permission: String) {
                    showNotification()
                }
            },
        )
    }
}

fun PermissionManager.withStoragePermission(withoutPermission: () -> Unit = {}, doAction: () -> Unit) {
    if (hasScopedStorage() || hasPermissionGranted(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
        doAction()
    } else {
        requestPermission(
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            object : PermissionManager.PermissionCallback {
                override fun onPermissionDenied(permission: String) {
                    withoutPermission()
                }

                override fun onPermissionGranted(permission: String) {
                    doAction()
                }
            },
        )
    }
}
