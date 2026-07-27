package com.mmax.fancontrol.hardware

import com.mmax.fancontrol.data.FanCurvePoint
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class TelemetrySnapshot(
    val thermal: ThermalSnapshot = ThermalSnapshot(),
    val fanPercent: Int = 0,
    val fanAdjustEnabled: Boolean = false,
    val activeCurveName: String = "",
    val activeCurvePoints: List<FanCurvePoint> = emptyList(),
)

/** One in-process source of truth shared by the dashboard, fan loop and overlay. */
object TelemetryRepository {
    private val mutable = MutableStateFlow(TelemetrySnapshot())
    val state: StateFlow<TelemetrySnapshot> = mutable.asStateFlow()

    fun updateThermal(
        thermal: ThermalSnapshot,
        fanPercent: Int,
        fanAdjustEnabled: Boolean,
        activeCurveName: String,
        activeCurvePoints: List<FanCurvePoint>,
    ) {
        mutable.update {
            it.copy(
                thermal = thermal,
                fanPercent = fanPercent.coerceIn(0, 100),
                fanAdjustEnabled = fanAdjustEnabled,
                activeCurveName = activeCurveName,
                activeCurvePoints = activeCurvePoints,
            )
        }
    }
}
