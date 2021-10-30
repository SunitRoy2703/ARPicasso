package com.sunit.arpicasso

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

/**
 * Helper to ask camera permission.
 */
object PermissionHelper {
    private const val CAMERA_PERMISSION = Manifest.permission.CAMERA
    private const val WRITE_PERMISSION = Manifest.permission.WRITE_EXTERNAL_STORAGE
    private const val READ_PERMISSION = Manifest.permission.READ_EXTERNAL_STORAGE
    private const val CAMERA_PERMISSION_CODE = 0
    private const val WRITE_PERMISSION_CODE = 1
    private const val READ_PERMISSION_CODE = 2

    /**
     * Check to see we have the necessary permissions for this app.
     */
    @JvmStatic
    fun hasCameraPermission(activity: Activity?): Boolean {
        return ContextCompat.checkSelfPermission(activity!!, CAMERA_PERMISSION) ==
                PackageManager.PERMISSION_GRANTED
    }

    /**
     * Check to see we have the necessary permissions for this app, and ask for them if we don't.
     */
    @JvmStatic
    fun requestCameraPermission(activity: Activity?) {
        ActivityCompat.requestPermissions(
            activity!!, arrayOf(CAMERA_PERMISSION),
            CAMERA_PERMISSION_CODE
        )
    }

    fun hasWritePermission(activity: Activity?): Boolean {
        return ContextCompat.checkSelfPermission(activity!!, WRITE_PERMISSION) ==
                PackageManager.PERMISSION_GRANTED
    }

    fun requestWritePermission(activity: Activity?) {
        ActivityCompat.requestPermissions(
            activity!!, arrayOf(WRITE_PERMISSION),
            WRITE_PERMISSION_CODE
        )
    }

    fun hasReadPermission(activity: Activity?): Boolean {
        return ContextCompat.checkSelfPermission(activity!!, READ_PERMISSION) ==
                PackageManager.PERMISSION_GRANTED
    }

    fun requestReadPermission(activity: Activity?) {
        ActivityCompat.requestPermissions(
            activity!!, arrayOf(READ_PERMISSION),
            READ_PERMISSION_CODE
        )
    }
}