package com.realbuds.app

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import com.realbuds.app.ui.theme.RealBudsTheme
import com.realbuds.app.ui.theme.ThemeMode
import com.realbuds.app.ui.theme.ThemePref
import androidx.core.content.ContextCompat
import android.content.pm.PackageManager
import com.realbuds.app.ui.BudsScreen
import com.realbuds.app.ui.BudsViewModel

class MainActivity : ComponentActivity() {

    private val vm = BudsViewModel()

    private val permLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { vm.refreshDevices(adapter()) }

    private fun adapter(): BluetoothAdapter? =
        (getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager)?.adapter

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        requestPermsIfNeeded()

        ThemePref.init(this)
        vm.initAdaptive(this)

        setContent {
            val mode by ThemePref.state
            val dark = when (mode) {
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }
            RealBudsTheme(darkTheme = dark) {
                BudsScreen(
                    vm = vm,
                    onRefresh = { vm.refreshDevices(adapter()) },
                )
            }
        }
        vm.refreshDevices(adapter())
    }

    private fun requestPermsIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val need = buildList {
                add(Manifest.permission.BLUETOOTH_CONNECT)
                add(Manifest.permission.BLUETOOTH_SCAN)
                // Adaptive ANC reads the step detector, which is permission
                // gated from Q onward. Declined is fine: the feature just
                // reports itself unavailable.
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    add(Manifest.permission.ACTIVITY_RECOGNITION)
                }
            }.filter {
                ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
            }
            if (need.isNotEmpty()) permLauncher.launch(need.toTypedArray())
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        vm.close()
    }
}
