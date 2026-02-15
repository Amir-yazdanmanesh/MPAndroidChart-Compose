package com.github.mikephil.charting.compose.gesture

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset

/**
 * Holds the mutable gesture state for a chart: zoom, pan, rotation, and highlight.
 *
 * Use [rememberChartGestureState] to create and remember an instance in a composable.
 *
 * @property scaleX Current X-axis zoom level (1f = no zoom)
 * @property scaleY Current Y-axis zoom level (1f = no zoom)
 * @property translationX Current horizontal pan offset in pixels
 * @property translationY Current vertical pan offset in pixels
 * @property rotationAngle Current rotation angle in degrees (for Pie/Radar)
 * @property highlight Currently highlighted value, or null
 * @property isDragging Whether the user is currently dragging
 * @property isScaling Whether the user is currently scaling (pinch)
 */
@Stable
class ChartGestureState(
    initialScaleX: Float = 1f,
    initialScaleY: Float = 1f,
    initialTranslationX: Float = 0f,
    initialTranslationY: Float = 0f,
    initialRotation: Float = 270f
) {
    var scaleX by mutableFloatStateOf(initialScaleX)
    var scaleY by mutableFloatStateOf(initialScaleY)
    var translationX by mutableFloatStateOf(initialTranslationX)
    var translationY by mutableFloatStateOf(initialTranslationY)
    var rotationAngle by mutableFloatStateOf(initialRotation)
    var highlight by mutableStateOf<ChartHighlight?>(null)
    var isDragging by mutableStateOf(false)
        internal set
    var isScaling by mutableStateOf(false)
        internal set

    /** Minimum zoom level */
    var minScaleX: Float = 1f
    var minScaleY: Float = 1f

    /** Maximum zoom level */
    var maxScaleX: Float = 20f
    var maxScaleY: Float = 20f

    /** Whether X-axis dragging is enabled */
    var dragXEnabled: Boolean = true

    /** Whether Y-axis dragging is enabled */
    var dragYEnabled: Boolean = true

    /** Whether X-axis scaling is enabled */
    var scaleXEnabled: Boolean = true

    /** Whether Y-axis scaling is enabled */
    var scaleYEnabled: Boolean = true

    /** Whether pinch zoom (both axes) is enabled */
    var pinchZoomEnabled: Boolean = true

    /** Whether double-tap zoom is enabled */
    var doubleTapToZoomEnabled: Boolean = true

    /** Whether highlighting on tap is enabled */
    var highlightPerTapEnabled: Boolean = true

    /** Whether highlighting while dragging is enabled */
    var highlightPerDragEnabled: Boolean = false

    /** Whether rotation is enabled (for Pie/Radar) */
    var rotationEnabled: Boolean = true

    /** Deceleration friction coefficient (0 = instant stop, 1 = infinite) */
    var dragDecelerationFrictionCoef: Float = 0.9f

    /**
     * Zoom in by the given factor at the given pivot point.
     */
    fun zoomIn(pivotX: Float = 0f, pivotY: Float = 0f, factor: Float = 1.4f) {
        if (scaleXEnabled) scaleX = (scaleX * factor).coerceIn(minScaleX, maxScaleX)
        if (scaleYEnabled) scaleY = (scaleY * factor).coerceIn(minScaleY, maxScaleY)
    }

    /**
     * Zoom out by the given factor at the given pivot point.
     */
    fun zoomOut(pivotX: Float = 0f, pivotY: Float = 0f, factor: Float = 1.4f) {
        if (scaleXEnabled) scaleX = (scaleX / factor).coerceIn(minScaleX, maxScaleX)
        if (scaleYEnabled) scaleY = (scaleY / factor).coerceIn(minScaleY, maxScaleY)
    }

    /**
     * Reset zoom and pan to default values.
     */
    fun resetZoom() {
        scaleX = 1f
        scaleY = 1f
        translationX = 0f
        translationY = 0f
    }

    /**
     * Clear the current highlight.
     */
    fun clearHighlight() {
        highlight = null
    }

    /**
     * Check if the chart is zoomed in.
     */
    val isZoomed: Boolean get() = scaleX > 1f || scaleY > 1f
}

/**
 * Creates and remembers a [ChartGestureState].
 *
 * @param initialScaleX Initial X zoom level
 * @param initialScaleY Initial Y zoom level
 * @param initialRotation Initial rotation angle (270 = top for Pie/Radar)
 * @return Remembered [ChartGestureState] instance
 */
@Composable
fun rememberChartGestureState(
    initialScaleX: Float = 1f,
    initialScaleY: Float = 1f,
    initialRotation: Float = 270f
): ChartGestureState {
    return remember {
        ChartGestureState(
            initialScaleX = initialScaleX,
            initialScaleY = initialScaleY,
            initialRotation = initialRotation
        )
    }
}
