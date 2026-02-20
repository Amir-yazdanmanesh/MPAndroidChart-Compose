package com.github.mikephil.charting.compose.utils

import com.github.mikephil.charting.compose.data.BarEntry
import com.github.mikephil.charting.compose.data.ChartEntry
import java.text.DecimalFormat

/**
 * Formats chart data values for display.
 */
fun interface ValueFormatter {
    /**
     * Formats a value for display on the chart.
     *
     * @param value The value to format
     * @return Formatted string
     */
    fun format(value: Float): String
}

/**
 * Default value formatter with configurable decimal places.
 *
 * @param decimalDigits Number of decimal places to show
 */
class DefaultValueFormatter(decimalDigits: Int = 1) : ValueFormatter {

    private val format: DecimalFormat

    init {
        val pattern = buildString {
            append("###,###,###,##0")
            if (decimalDigits > 0) {
                append(".")
                repeat(decimalDigits) { append("0") }
            }
        }
        format = DecimalFormat(pattern)
    }

    override fun format(value: Float): String = format.format(value)
}

/**
 * Formats values as percentages (e.g., "42.5 %").
 *
 * @param decimalFormat Optional custom decimal format
 */
class PercentFormatter(
    private val decimalFormat: DecimalFormat = DecimalFormat("###,###,##0.0")
) : ValueFormatter {

    override fun format(value: Float): String = "${decimalFormat.format(value)} %"
}

/**
 * Formats large numbers with abbreviation suffixes.
 *
 * Examples: 5821 → "5.8k", 2000000 → "2m", 1500000000 → "1.5b"
 *
 * @param suffixes Custom suffix array (default: "", "k", "m", "b", "t")
 * @param appendix Extra text appended after the suffix
 * @param maxLength Maximum output string length
 */
class LargeValueFormatter(
    private val suffixes: Array<String> = arrayOf("", "k", "m", "b", "t"),
    private val appendix: String = "",
    private val maxLength: Int = 5
) : ValueFormatter {

    override fun format(value: Float): String {
        if (value == 0f) return "0$appendix"

        val absValue = kotlin.math.abs(value)
        val sign = if (value < 0) "-" else ""

        // Find appropriate suffix
        var reducedValue = absValue
        var suffixIndex = 0
        while (reducedValue >= 1000f && suffixIndex < suffixes.size - 1) {
            reducedValue /= 1000f
            suffixIndex++
        }

        // Format with limited length
        val formatted = makePretty(reducedValue)
        return "$sign$formatted${suffixes[suffixIndex]}$appendix"
    }

    private fun makePretty(value: Float): String {
        val str = String.format("%.2f", value)
        // Trim trailing zeros but keep at least one decimal
        val result = str.trimEnd('0').trimEnd('.')
        return if (result.length > maxLength) {
            String.format("%.0f", value)
        } else {
            result
        }
    }
}

/**
 * Maps numeric index values to string labels.
 *
 * Useful for categorical X-axis labels.
 *
 * @param labels The labels to display at each index
 */
class IndexValueFormatter(
    private var labels: List<String> = emptyList()
) : ValueFormatter {

    constructor(vararg labels: String) : this(labels.toList())

    override fun format(value: Float): String {
        val index = value.toInt()
        return if (index >= 0 && index < labels.size) labels[index] else ""
    }

    fun setLabels(newLabels: List<String>) {
        labels = newLabels
    }

    fun getLabels(): List<String> = labels
}

/**
 * Formatter for stacked bar chart values.
 *
 * Can show either all stack segment values or only the total on top.
 *
 * @param drawWholeStack Whether to show values for every stack segment
 * @param appendix Text to append (e.g., " $")
 * @param decimalDigits Number of decimal places
 */
class StackedValueFormatter(
    private val drawWholeStack: Boolean = false,
    private val appendix: String = "",
    decimalDigits: Int = 1
) : ValueFormatter {

    private val format: DecimalFormat

    init {
        val pattern = buildString {
            append("###,###,###,##0")
            if (decimalDigits > 0) {
                append(".")
                repeat(decimalDigits) { append("0") }
            }
        }
        format = DecimalFormat(pattern)
    }

    override fun format(value: Float): String {
        return "${format.format(value)}$appendix"
    }

    /**
     * Formats a stacked bar entry value, optionally showing only the top value.
     *
     * @param value The value to format
     * @param entry The bar entry
     * @param stackIndex The index within the stack
     * @return Formatted string, or empty if this segment shouldn't show a value
     */
    fun formatStacked(value: Float, entry: BarEntry, stackIndex: Int): String {
        if (!drawWholeStack) {
            val yValues = entry.yValues ?: return format(value) + appendix
            // Only show value for top stack segment
            if (stackIndex < yValues.size - 1) return ""
        }
        return "${format.format(value)}$appendix"
    }
}
