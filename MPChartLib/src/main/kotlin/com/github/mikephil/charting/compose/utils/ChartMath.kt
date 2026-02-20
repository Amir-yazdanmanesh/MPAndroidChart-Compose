package com.github.mikephil.charting.compose.utils

import androidx.compose.ui.geometry.Offset
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Mathematical utility functions for chart calculations.
 */
object ChartMath {

    const val DEG2RAD: Float = (Math.PI / 180.0).toFloat()
    const val RAD2DEG: Float = (180.0 / Math.PI).toFloat()
    const val FLOAT_EPSILON: Float = Float.MIN_VALUE

    private val POW_10 = intArrayOf(
        1, 10, 100, 1000, 10000, 100000, 1000000, 10000000, 100000000, 1000000000
    )

    /**
     * Calculates a position on a circle at a given angle and distance from center.
     *
     * @param center Center point of the circle
     * @param dist Distance from center (radius)
     * @param angle Angle in degrees
     * @return Position on the circle
     */
    fun getPosition(center: Offset, dist: Float, angle: Float): Offset {
        val rad = angle * DEG2RAD
        return Offset(
            x = center.x + dist * cos(rad),
            y = center.y + dist * sin(rad)
        )
    }

    /**
     * Normalizes an angle to the range [0, 360).
     */
    fun normalizeAngle(angle: Float): Float {
        var a = angle % 360f
        if (a < 0f) a += 360f
        return a
    }

    /**
     * Calculates the distance between two points.
     */
    fun distance(x1: Float, y1: Float, x2: Float, y2: Float): Float {
        val dx = x2 - x1
        val dy = y2 - y1
        return sqrt(dx * dx + dy * dy)
    }

    /**
     * Calculates the distance between two offsets.
     */
    fun distance(a: Offset, b: Offset): Float {
        return distance(a.x, a.y, b.x, b.y)
    }

    /**
     * Rounds the given number to the next significant number.
     *
     * For example: 0.00234 → 0.003, 123 → 200, 0.456 → 0.5
     */
    fun roundToNextSignificant(value: Double): Float {
        if (java.lang.Double.isInfinite(value) || java.lang.Double.isNaN(value) || value == 0.0) {
            return 0f
        }

        val d = ceil(log10(if (value < 0) -value else value)).toFloat()
        val pw = 1 - d.toInt()
        val magnitude = 10.0.pow(pw.toDouble()).toFloat()
        val shifted = (value * magnitude).toLong()
        return shifted / magnitude
    }

    /**
     * Returns the number of decimal places needed for the given value.
     */
    fun getDecimals(value: Float): Int {
        val i = roundToNextSignificant(value.toDouble())
        if (java.lang.Float.isInfinite(i)) return 0
        return (-floor(log10(abs(i).toDouble()))).toInt().coerceAtLeast(0)
    }

    /**
     * Computes evenly spaced values between min and max with "nice" numbers.
     *
     * Uses a label-spacing algorithm that produces clean axis labels.
     *
     * @param min Data minimum
     * @param max Data maximum
     * @param labelCount Desired number of labels
     * @return List of evenly spaced values
     */
    fun computeNiceAxisValues(min: Float, max: Float, labelCount: Int): List<Float> {
        val count = labelCount.coerceAtLeast(2)
        val range = abs(max - min).toDouble()

        if (range <= 0) {
            return listOf(min)
        }

        // Find a "nice" step size
        val rawInterval = range / (count - 1)
        val interval = roundToNextSignificant(rawInterval).toDouble()

        if (interval <= 0) {
            return List(count) { min + it * (range / (count - 1)).toFloat() }
        }

        // Snap min/max to interval
        val first = floor(min / interval) * interval
        val last = ceil(max / interval) * interval

        val values = mutableListOf<Float>()
        var v = first
        while (v <= last + interval * 0.5) {
            values.add(v.toFloat())
            v += interval
        }

        return values
    }

    /**
     * Linearly interpolates between two values.
     *
     * @param start Start value
     * @param end End value
     * @param fraction Fraction between 0 and 1
     * @return Interpolated value
     */
    fun lerp(start: Float, end: Float, fraction: Float): Float {
        return start + (end - start) * fraction
    }

    /**
     * Linearly interpolates between two offsets.
     */
    fun lerp(start: Offset, end: Offset, fraction: Float): Offset {
        return Offset(
            x = lerp(start.x, end.x, fraction),
            y = lerp(start.y, end.y, fraction)
        )
    }

    /**
     * Clamps a value between min and max.
     */
    fun clamp(value: Float, min: Float, max: Float): Float {
        return value.coerceIn(min, max)
    }

    /**
     * Returns the closest power of 10 at or below the given value.
     *
     * For example: 123 → 100, 0.45 → 0.1
     */
    fun floorPow10(value: Float): Float {
        if (value <= 0f) return 1f
        val exp = floor(log10(value)).toInt()
        return 10f.pow(exp)
    }

    /**
     * Calculates the dimensions of a rotated rectangle.
     *
     * @param width Original width
     * @param height Original height
     * @param degrees Rotation angle in degrees
     * @return Pair of (rotated width, rotated height)
     */
    fun sizeOfRotatedRectangle(width: Float, height: Float, degrees: Float): Pair<Float, Float> {
        val rad = degrees * DEG2RAD
        val cosA = abs(cos(rad))
        val sinA = abs(sin(rad))
        return Pair(
            width * cosA + height * sinA,
            width * sinA + height * cosA
        )
    }
}
