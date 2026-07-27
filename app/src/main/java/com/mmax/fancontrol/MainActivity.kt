package com.mmax.fancontrol

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.core.content.ContextCompat
import com.mmax.fancontrol.data.Prefs
import com.mmax.fancontrol.service.SystemControlService
import com.mmax.fancontrol.theme.FanControlTheme
import com.mmax.fancontrol.ui.DashboardScreen

class MainActivity : ComponentActivity() {
    private val notificationPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val prefs = getSharedPreferences(Prefs.FILE, Context.MODE_PRIVATE)
        var showRootNotice by mutableStateOf(
            !prefs.getBoolean(Prefs.ROOT_NOTICE_ACKNOWLEDGED, false)
        )
        setContent {
            FanControlTheme {
                DashboardScreen(
                    onFanCurveSelected = ::onFanCurveSelected,
                    onRefreshRoot = { requestRoot(forceRefresh = true) },
                )
                if (showRootNotice) {
                    AlertDialog(
                        onDismissRequest = {},
                        title = { Text(stringResource(R.string.root_permission)) },
                        text = { Text(stringResource(R.string.root_notice_message)) },
                        confirmButton = {
                            TextButton(
                                onClick = {
                                    prefs.edit()
                                        .putBoolean(Prefs.ROOT_NOTICE_ACKNOWLEDGED, true)
                                        .apply()
                                    showRootNotice = false
                                    requestRoot(forceRefresh = true)
                                }
                            ) {
                                Text(stringResource(R.string.confirm))
                            }
                        },
                    )
                }
            }
        }
        if (!showRootNotice) {
            requestRoot(forceRefresh = false)
        }
    }

    private fun requestRoot(forceRefresh: Boolean) {
        RootAccessManager.ensureRoot(forceRefresh = forceRefresh) {
            SystemControlService.startOrUpdate(applicationContext)
        }
    }

    private fun onFanCurveSelected(enabled: Boolean) {
        if (!enabled || Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        if (
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
        ) return

        val prefs = getSharedPreferences(Prefs.FILE, Context.MODE_PRIVATE)
        if (prefs.getBoolean(Prefs.NOTIFICATION_PERMISSION_REQUESTED, false)) return
        prefs.edit()
            .putBoolean(Prefs.NOTIFICATION_PERMISSION_REQUESTED, true)
            .apply()
        notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
    }
}
