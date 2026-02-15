package com.github.mikephil.charting.compose.gesture

import androidx.compose.ui.geometry.Offset
import com.github.mikephil.charting.compose.data.BarData
import com.github.mikephil.charting.compose.data.BarEntry
import com.github.mikephil.charting.compose.data.BubbleData
import com.github.mikephil.charting.compose.data.CandleData
import com.github.mikephil.charting.compose.data.ChartData
import com.github.mikephil.charting.compose.data.ChartDataSet
import com.github.mikephil.charting.compose.data.ChartEntry
import com.github.mikephil.charting.compose.data.Entry
import com.github.mikephil.charting.compose.data.LineData
import com.github.mikephil.charting.compose.data.PieData
import com.github.mikephil.charting.compose.data.PieEntry
import com.github.mikephil.charting.compose.data.ScatterData
import com.github.mikephil.charting.compose.renderer.ChartViewport
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.sqrt

/**
 * Represents a highlighted value in the chart.
 *
 * @property x X value of the highlighted entry
 * @property y Y value of the highlighted entry
 * @property dataSetIndex Index of the dataset containing the highlighted entry
 * @property entryIndex Index of the entry within the dataset
 * @property stackIndex Stack index for stacked bars (-1 if not stacked)
 * @property pixelPosition Screen position of the highlighted point
 */
data class ChartHighlight(
    val x: Float = 0f,
    val y: Float = 0f,
    val dataSetIndex: Int = 0,
    val entryIndex: Int = 0,
    val stackIndex: Int = -1,
    val pixelPosition: Offset = Offset.Zero
) {
    val isStacked: Boolean get() = stackIndex >= 0
}

/**
 * Finds the closest data point to the given touch position for axis-based charts.
 *
 * Works with LineData, ScatterData, BubbleData, and CandleData.
 *
 * @param touchPosition Touch position in pixels
 * @param data Chart data containing datasets
 * @param viewport The chart viewport for coordinate mapping
 * @param maxDistancePx Maximum distance in pixels to consider a match
 * @return The closest highlight, or null if no point is within range
 */
fun <S : ChartDataSet<out ChartEntry>> findClosestHighlight(
    touchPosition: Offset,
    data: ChartData<S>,
    viewport: ChartViewport,
    maxDistancePx: Float = Float.MAX_VALUE
): ChartHighlight? {
    if (data.isEmpty) return null

    var bestHighlight: ChartHighlight? = null
    var bestDistance = maxDistancePx

    for ((dsIndex, dataSet) in data.dataSets.withIndex()) {
        if (!dataSet.visible || !dataSet.highlightEnabled) continue

        for ((entryIndex, entry) in dataSet.entries.withIndex()) {
            val px = when (entry) {
                is Entry -> viewport.dataToPixel(entry.x, entry.y)
                is com.github.mikephil.charting.compose.data.BubbleEntry ->
                    viewport.dataToPixel(entry.x, entry.y)
                is com.github.mikephil.charting.compose.data.CandleEntry ->
                    viewport.dataToPixel(entry.x, (entry.high + entry.low) / 2f)
                else -> continue
            }

            val dist = distance(touchPosition, px)
            if (dist < bestDistance) {
                bestDistance = dist
                bestHighlight = ChartHighlight(
                    x = when (entry) {
                        is Entry -> entry.x
                        is com.github.mikephil.charting.compose.data.BubbleEntry -> entry.x
                        is com.github.mikephil.charting.compose.data.CandleEntry -> entry.x
                        else -> 0f
                    },
                    y = entry.y,
                    dataSetIndex = dsIndex,
                    entryIndex = entryIndex,
                    pixelPosition = px
                )
            }
        }
    }

    return bestHighlight
}

/**
 * Finds the closest bar entry to the given touch position.
 *
 * Uses X-axis distance only, matching the original BarHighlighter behavior.
 *
 * @param touchPosition Touch position in pixels
 * @param data Bar chart data
 * @param viewport The chart viewport for coordinate mapping
 * @return The closest bar highlight, or null if no bar is found
 */
fun findBarHighlight(
    touchPosition: Offset,
    data: BarData,
    viewport: ChartViewport
): ChartHighlight? {
    if (data.isEmpty) return null

    val touchDataX = viewport.pixelToDataX(touchPosition.x)
    var bestHighlight: ChartHighlight? = null
    var bestDistance = Float.MAX_VALUE

    for ((dsIndex, dataSet) in data.dataSets.withIndex()) {
        if (!dataSet.visible || !dataSet.highlightEnabled) continue

        for ((entryIndex, entry) in dataSet.entries.withIndex()) {
            val dist = abs(entry.x - touchDataX)
            if (dist < bestDistance) {
                bestDistance = dist

                // Determine stack index
                val stackIndex = if (entry.yValues != null) {
                    val touchDataY = viewport.pixelToDataY(touchPosition.y)
                    findStackIndex(entry, touchDataY)
                } else -1

                val px = viewport.dataToPixel(entry.x, entry.y)
                bestHighlight = ChartHighlight(
                    x = entry.x,
                    y = entry.y,
                    dataSetIndex = dsIndex,
                    entryIndex = entryIndex,
                    stackIndex = stackIndex,
                    pixelPosition = px
                )
            }
        }
    }

    return bestHighlight
}

/**
 * Finds the pie slice at the given touch position.
 *
 * @param touchPosition Touch position in pixels
 * @param data Pie chart data
 * @param center Center of the pie chart
 * @param radius Outer radius of the pie
 * @param holeRadius Inner hole radius (0 = solid pie)
 * @param rotationAngle Starting rotation angle
 * @return The highlighted slice, or null if touch is outside the pie
 */
fun findPieHighlight(
    touchPosition: Offset,
    data: PieData,
    center: Offset,
    radius: Float,
    holeRadius: Float = 0f,
    rotationAngle: Float = 270f
): ChartHighlight? {
    val dataSet = data.dataSet ?: return null
    if (dataSet.entries.isEmpty()) return null

    val dx = touchPosition.x - center.x
    val dy = touchPosition.y - center.y
    val dist = sqrt(dx * dx + dy * dy)

    // Check if touch is within the pie ring
    if (dist > radius || dist < holeRadius) return null

    // Calculate angle (0 = right, clockwise)
    var angle = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()
    if (angle < 0) angle += 360f

    // Adjust for rotation
    var adjustedAngle = (angle - rotationAngle % 360f + 360f) % 360f

    // Find slice at angle
    val total = dataSet.entries.sumOf { it.value.toDouble() }.toFloat()
    if (total == 0f) return null

    var cumulative = 0f
    for ((index, entry) in dataSet.entries.withIndex()) {
        val sliceAngle = entry.value / total * 360f
        if (adjustedAngle >= cumulative && adjustedAngle < cumulative + sliceAngle) {
            return ChartHighlight(
                x = index.toFloat(),
                y = entry.value,
                dataSetIndex = 0,
                entryIndex = index,
                pixelPosition = touchPosition
            )
        }
        cumulative += sliceAngle
    }

    return null
}

/**
 * Finds the radar value closest to the touch position.
 *
 * @param touchPosition Touch position in pixels
 * @param axisCount Number of radar axes
 * @param center Center of the radar chart
 * @param radius Outer radius
 * @param rotationAngle Starting rotation angle
 * @return The closest axis index, or -1 if touch is outside
 */
fun findRadarHighlightIndex(
    touchPosition: Offset,
    axisCount: Int,
    center: Offset,
    radius: Float,
    rotationAngle: Float = 270f
): Int {
    if (axisCount == 0) return -1

    val dx = touchPosition.x - center.x
    val dy = touchPosition.y - center.y
    val dist = sqrt(dx * dx + dy * dy)

    if (dist > radius * 1.2f) return -1

    var angle = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()
    if (angle < 0) angle += 360f

    val adjustedAngle = (angle - rotationAngle % 360f + 360f) % 360f
    val sliceAngle = 360f / axisCount

    return ((adjustedAngle + sliceAngle / 2f) % 360f / sliceAngle).toInt().coerceIn(0, axisCount - 1)
}

private fun findStackIndex(entry: BarEntry, touchY: Float): Int {
    val ranges = entry.ranges ?: return -1
    for ((index, range) in ranges.withIndex()) {
        if (touchY in range) return index
    }
    return -1
}

private fun distance(a: Offset, b: Offset): Float {
    val dx = a.x - b.x
    val dy = a.y - b.y
    return sqrt(dx * dx + dy * dy)
}
