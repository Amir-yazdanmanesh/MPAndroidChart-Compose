package com.github.mikephil.charting.compose.gesture

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged

/**
 * The type of chart gesture being performed.
 */
enum class ChartGesture {
    NONE, DRAG, X_ZOOM, Y_ZOOM, PINCH_ZOOM, ROTATE,
    SINGLE_TAP, DOUBLE_TAP, LONG_PRESS, FLING
}

/**
 * Listener interface for chart gesture events.
 */
interface ChartGestureListener {
    /** Called when a gesture begins. */
    fun onGestureStart(gesture: ChartGesture) {}

    /** Called when a gesture ends. */
    fun onGestureEnd(gesture: ChartGesture) {}

    /** Called on single tap. */
    fun onSingleTap(position: Offset) {}

    /** Called on double tap. */
    fun onDoubleTap(position: Offset) {}

    /** Called on long press. */
    fun onLongPress(position: Offset) {}

    /** Called during drag with delta. */
    fun onDrag(position: Offset, dragAmount: Offset) {}

    /** Called during pinch zoom with scale factors. */
    fun onScale(centroid: Offset, scaleX: Float, scaleY: Float) {}

    /** Called when a value is highlighted. */
    fun onValueSelected(highlight: ChartHighlight) {}

    /** Called when highlight is cleared. */
    fun onNothingSelected() {}
}

/**
 * Adds tap gesture detection to a chart composable.
 *
 * Handles single tap (for highlighting), double tap (for zoom), and long press.
 *
 * @param state The chart gesture state to update
 * @param onTap Callback invoked with the tap position (for highlight resolution)
 * @param listener Optional gesture listener for event callbacks
 */
fun Modifier.chartTapGestures(
    state: ChartGestureState,
    onTap: ((Offset) -> Unit)? = null,
    listener: ChartGestureListener? = null
): Modifier = this.pointerInput(state) {
    detectTapGestures(
        onTap = { offset ->
            listener?.onSingleTap(offset)
            if (state.highlightPerTapEnabled) {
                onTap?.invoke(offset)
            }
        },
        onDoubleTap = { offset ->
            listener?.onDoubleTap(offset)
            if (state.doubleTapToZoomEnabled) {
                if (state.isZoomed) {
                    state.resetZoom()
                } else {
                    state.zoomIn()
                }
            }
        },
        onLongPress = { offset ->
            listener?.onLongPress(offset)
        }
    )
}

/**
 * Adds drag and pinch-zoom gesture detection for axis-based charts.
 *
 * Handles panning (single-finger drag) and zooming (two-finger pinch).
 *
 * @param state The chart gesture state to update
 * @param listener Optional gesture listener for event callbacks
 */
fun Modifier.chartTransformGestures(
    state: ChartGestureState,
    listener: ChartGestureListener? = null
): Modifier = this.pointerInput(state) {
    awaitEachGesture {
        val firstDown = awaitFirstDown(requireUnconsumed = false)
        var currentGesture = ChartGesture.NONE

        do {
            val event = awaitPointerEvent()
            val pointers = event.changes.filter { it.pressed }

            when {
                pointers.size >= 2 -> {
                    // Pinch zoom
                    if (currentGesture != ChartGesture.PINCH_ZOOM) {
                        currentGesture = ChartGesture.PINCH_ZOOM
                        state.isScaling = true
                        state.isDragging = false
                        listener?.onGestureStart(ChartGesture.PINCH_ZOOM)
                    }

                    val zoom = event.calculateZoom()
                    val centroid = event.calculateCentroid()

                    if (zoom != 1f) {
                        val newScaleX = if (state.scaleXEnabled && state.pinchZoomEnabled)
                            (state.scaleX * zoom).coerceIn(state.minScaleX, state.maxScaleX)
                        else state.scaleX

                        val newScaleY = if (state.scaleYEnabled && state.pinchZoomEnabled)
                            (state.scaleY * zoom).coerceIn(state.minScaleY, state.maxScaleY)
                        else state.scaleY

                        state.scaleX = newScaleX
                        state.scaleY = newScaleY
                        listener?.onScale(centroid, newScaleX, newScaleY)
                    }

                    event.changes.forEach { if (it.positionChanged()) it.consume() }
                }

                pointers.size == 1 && event.type == PointerEventType.Move -> {
                    // Drag
                    if (currentGesture != ChartGesture.DRAG) {
                        currentGesture = ChartGesture.DRAG
                        state.isDragging = true
                        state.isScaling = false
                        listener?.onGestureStart(ChartGesture.DRAG)
                    }

                    val pan = event.calculatePan()
                    if (pan != Offset.Zero) {
                        if (state.dragXEnabled) state.translationX += pan.x
                        if (state.dragYEnabled) state.translationY += pan.y
                        listener?.onDrag(pointers.first().position, pan)
                    }

                    event.changes.forEach { if (it.positionChanged()) it.consume() }
                }
            }
        } while (event.changes.any { it.pressed })

        // Gesture ended
        if (currentGesture != ChartGesture.NONE) {
            listener?.onGestureEnd(currentGesture)
        }
        state.isDragging = false
        state.isScaling = false
    }
}

/**
 * Adds rotation gesture detection for Pie and Radar charts.
 *
 * Single-finger drag rotates the chart around its center.
 *
 * @param state The chart gesture state to update
 * @param center Center point of the chart (for angle calculation)
 * @param listener Optional gesture listener for event callbacks
 */
fun Modifier.chartRotationGestures(
    state: ChartGestureState,
    center: Offset,
    listener: ChartGestureListener? = null
): Modifier = this.pointerInput(state, center) {
    awaitEachGesture {
        val firstDown = awaitFirstDown(requireUnconsumed = false)

        if (!state.rotationEnabled) return@awaitEachGesture

        var previousAngle = angleTo(center, firstDown.position)
        var gestureStarted = false

        do {
            val event = awaitPointerEvent()
            val pointer = event.changes.firstOrNull { it.pressed } ?: break

            if (pointer.positionChanged()) {
                val currentAngle = angleTo(center, pointer.position)
                val deltaAngle = currentAngle - previousAngle

                if (!gestureStarted) {
                    gestureStarted = true
                    listener?.onGestureStart(ChartGesture.ROTATE)
                }

                state.rotationAngle += deltaAngle
                previousAngle = currentAngle
                pointer.consume()
            }
        } while (event.changes.any { it.pressed })

        if (gestureStarted) {
            listener?.onGestureEnd(ChartGesture.ROTATE)
        }
    }
}

/**
 * Combines tap, drag, and pinch gestures for axis-based charts (Line, Bar, Scatter, etc.).
 *
 * @param state The chart gesture state
 * @param onTap Callback for tap position (used for highlight resolution)
 * @param listener Optional gesture listener
 */
fun Modifier.axisChartGestures(
    state: ChartGestureState,
    onTap: ((Offset) -> Unit)? = null,
    listener: ChartGestureListener? = null
): Modifier = this
    .chartTapGestures(state = state, onTap = onTap, listener = listener)
    .chartTransformGestures(state = state, listener = listener)

/**
 * Combines tap and rotation gestures for circular charts (Pie, Radar).
 *
 * @param state The chart gesture state
 * @param center Center point of the chart
 * @param onTap Callback for tap position (used for highlight resolution)
 * @param listener Optional gesture listener
 */
fun Modifier.circularChartGestures(
    state: ChartGestureState,
    center: Offset,
    onTap: ((Offset) -> Unit)? = null,
    listener: ChartGestureListener? = null
): Modifier = this
    .chartTapGestures(state = state, onTap = onTap, listener = listener)
    .chartRotationGestures(state = state, center = center, listener = listener)

private fun angleTo(center: Offset, point: Offset): Float {
    val dx = point.x - center.x
    val dy = point.y - center.y
    var angle = Math.toDegrees(kotlin.math.atan2(dy.toDouble(), dx.toDouble())).toFloat()
    if (angle < 0) angle += 360f
    return angle
}
