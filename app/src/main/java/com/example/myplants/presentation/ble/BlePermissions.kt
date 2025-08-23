package com.example.myplants.presentation.ble

import android.Manifest
import android.content.Context
import android.os.Build
import androidx.core.content.ContextCompat
import android.content.pm.PackageManager

object BlePermissions {
    fun required(): Array<String> =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT
            )
        } else {
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
        }

    fun hasAll(context: Context): Boolean =
        required().all { perm ->
            ContextCompat.checkSelfPermission(context, perm) == PackageManager.PERMISSION_GRANTED
        }
}