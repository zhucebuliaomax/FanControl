package com.mmax.fancontrol.tile

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.RadioButtonChecked
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mmax.fancontrol.MainActivity
import com.mmax.fancontrol.R
import com.mmax.fancontrol.RootAccessManager
import com.mmax.fancontrol.data.FanCurvePreferences
import com.mmax.fancontrol.data.Prefs
import com.mmax.fancontrol.data.displayName
import com.mmax.fancontrol.service.SystemControlService
import com.mmax.fancontrol.theme.FanControlTheme
import androidx.core.content.ContextCompat

/** Routes Quick Settings long presses and renders the fan chooser as a real dialog window. */
class FanCurveTilePreferencesActivity : ComponentActivity() {
    private val notificationPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {
        finish()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        @Suppress("DEPRECATION")
        overridePendingTransition(0, 0)

        if (originatingTile()?.className == OverlayTileService::class.java.name) {
            startActivity(
                Intent(this, MainActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            )
            finish()
            return
        }

        window.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        window.setDimAmount(0.32f)
        window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
        window.setGravity(Gravity.CENTER)
        setFinishOnTouchOutside(true)

        setContent {
            FanControlTheme {
                val config by remember { mutableStateOf(currentConfig()) }
                Surface(
                    modifier = Modifier.width(300.dp),
                    shape = MaterialTheme.shapes.extraLarge,
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                ) {
                    Column(Modifier.padding(top = 18.dp, bottom = 8.dp)) {
                        Text(
                            text = stringResource(R.string.select_fan_curve),
                            modifier = Modifier.padding(horizontal = 20.dp),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Spacer(Modifier.height(8.dp))
                        config.catalog.profiles.forEach { profile ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(44.dp)
                                    .clickable { select(profile.id) }
                                    .padding(horizontal = 20.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(
                                    imageVector = if (profile.id == config.activeProfileId) {
                                        Icons.Default.RadioButtonChecked
                                    } else {
                                        Icons.Default.RadioButtonUnchecked
                                    },
                                    contentDescription = null,
                                    tint = if (profile.id == config.activeProfileId) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    },
                                    modifier = Modifier.size(21.dp),
                                )
                                Spacer(Modifier.width(12.dp))
                                Text(
                                    text = profile.displayName(this@FanCurveTilePreferencesActivity),
                                    style = MaterialTheme.typography.bodyLarge,
                                )
                            }
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp)
                                .clickable { finish() }
                                .padding(horizontal = 20.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Spacer(Modifier.weight(1f))
                            Text(
                                text = stringResource(R.string.cancel),
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Medium,
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        window.setLayout(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
        )
    }

    override fun finish() {
        super.finish()
        @Suppress("DEPRECATION")
        overridePendingTransition(0, 0)
    }

    private fun originatingTile(): ComponentName? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(Intent.EXTRA_COMPONENT_NAME, ComponentName::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(Intent.EXTRA_COMPONENT_NAME)
        }

    private fun currentConfig() =
        FanCurvePreferences.load(getSharedPreferences(Prefs.FILE, Context.MODE_PRIVATE))

    private fun select(profileId: String) {
        FanCurvePreferences.select(
            getSharedPreferences(Prefs.FILE, Context.MODE_PRIVATE),
            profileId,
        )
        FanQuickSettingsTile.requestRefresh(this)
        RootAccessManager.ensureRoot {
            SystemControlService.startOrUpdate(applicationContext)
            if (!requestNotificationPermissionIfNeeded()) {
                finish()
            }
        }
    }

    private fun requestNotificationPermissionIfNeeded(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return false
        if (
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
        ) return false

        val prefs = getSharedPreferences(Prefs.FILE, Context.MODE_PRIVATE)
        if (prefs.getBoolean(Prefs.NOTIFICATION_PERMISSION_REQUESTED, false)) return false
        prefs.edit()
            .putBoolean(Prefs.NOTIFICATION_PERMISSION_REQUESTED, true)
            .apply()
        notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        return true
    }
}
