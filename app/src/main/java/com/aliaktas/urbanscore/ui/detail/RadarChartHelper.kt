package com.aliaktas.urbanscore.ui.detail

import android.content.Context
import android.graphics.Color
import androidx.annotation.ColorInt
import com.aliaktas.urbanscore.R
import com.aliaktas.urbanscore.data.model.CategoryRatings
import com.github.mikephil.charting.charts.RadarChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.RadarData
import com.github.mikephil.charting.data.RadarDataSet
import com.github.mikephil.charting.data.RadarEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * Helper class to manage radar chart operations.
 * Handles chart setup, data processing and updates.
 */
class RadarChartHelper @Inject constructor(private val context: Context) {

    // Chart appearance constants
    companion object {
        private val CATEGORIES = arrayOf("View", "Safety", "Livability", "Cost", "Social")

        @ColorInt private val PRIMARY_COLOR = Color.parseColor("#DF21F398")
        @ColorInt private val STROKE_COLOR = Color.parseColor("#1BA4C6")
        @ColorInt private val TEXT_COLOR = Color.parseColor("#1BA4C6")
        @ColorInt private val WEB_COLOR = Color.parseColor("#33FFFFFF")
        @ColorInt private val WEB_COLOR_INNER = Color.parseColor("#22FFFFFF")

        private const val WEB_LINE_WIDTH = 0.7f
        private const val WEB_LINE_WIDTH_INNER = 0.5f
        private const val WEB_ALPHA = 70

        private const val CHART_MIN_OFFSET = 65f
        private const val AXIS_TEXT_SIZE = 12f
        private const val AXIS_VALUE_TEXT_SIZE = 9f

        private const val AXIS_MIN = 0f
        private const val AXIS_MAX = 10f

        private const val DATASET_FILL_ALPHA = 110
        private const val DATASET_LINE_WIDTH = 1.6f
        private const val DATASET_VALUE_TEXT_SIZE = 12f
        private const val DATASET_HIGHLIGHT_STROKE_WIDTH = 1f

        private const val ANIMATION_DURATION = 1500
    }

    /**
     * Setup the radar chart with initial configuration.
     * This should be called once when the chart is first initialized.
     */
    suspend fun setupRadarChart(chart: RadarChart) = withContext(Dispatchers.Main) {
        chart.apply {
            // Chart configuration
            description.isEnabled = false
            webLineWidth = WEB_LINE_WIDTH
            webColor = WEB_COLOR
            webLineWidthInner = WEB_LINE_WIDTH_INNER
            webColorInner = WEB_COLOR_INNER
            webAlpha = WEB_ALPHA

            // Layout configuration
            minOffset = CHART_MIN_OFFSET
            setExtraOffsets(0f, 0f, 0f, 0f)

            // X axis labels
            xAxis.apply {
                textSize = AXIS_TEXT_SIZE
                textColor = TEXT_COLOR
                yOffset = 1f
                xOffset = 0f
                typeface = context.resources.getFont(R.font.poppins_medium)
                valueFormatter = IndexAxisValueFormatter(CATEGORIES)
            }

            // Y axis (values axis)
            yAxis.apply {
                setLabelCount(6, true)
                textColor = TEXT_COLOR
                textSize = AXIS_VALUE_TEXT_SIZE
                axisMinimum = AXIS_MIN
                axisMaximum = AXIS_MAX
                setDrawLabels(false)
                typeface = context.resources.getFont(R.font.poppins_medium)
            }

            // Legend
            legend.isEnabled = false
        }
    }

    /**
     * Process city ratings and update chart data.
     * Processing happens on background thread, UI update on main thread.
     *
     * @param chart The RadarChart instance to update
     * @param ratings The category ratings data to display
     */
    suspend fun updateChartData(chart: RadarChart, ratings: CategoryRatings) {
        // Calculate chart data on IO thread
        val chartData = withContext(Dispatchers.Default) {
            createChartData(ratings)
        }

        // Update UI on Main thread
        withContext(Dispatchers.Main) {
            // Apply chart data and animate
            chart.data = chartData
            chart.invalidate()
            chart.animateXY(ANIMATION_DURATION, ANIMATION_DURATION)
        }
    }

    /**
     * Create radar chart data from category ratings.
     * This is a CPU-intensive operation, so it runs on background thread.
     *
     * @param ratings The category ratings data to convert to chart data
     * @return RadarData object ready to be displayed
     */
    private fun createChartData(ratings: CategoryRatings): RadarData {
        // Convert ratings to radar entries
        val entries = ArrayList<RadarEntry>().apply {
            add(RadarEntry(ratings.environment.toFloat()))
            add(RadarEntry(ratings.safety.toFloat()))
            add(RadarEntry(ratings.livability.toFloat()))
            add(RadarEntry(ratings.cost.toFloat()))
            add(RadarEntry(ratings.social.toFloat()))
        }

        // Create and style data set
        val dataSet = RadarDataSet(entries, "").apply {
            color = STROKE_COLOR
            fillColor = PRIMARY_COLOR
            setDrawFilled(true)
            fillAlpha = DATASET_FILL_ALPHA
            lineWidth = DATASET_LINE_WIDTH
            valueTextColor = Color.WHITE
            valueTextSize = DATASET_VALUE_TEXT_SIZE
            isDrawHighlightCircleEnabled = true
            setDrawHighlightIndicators(false)
            highlightCircleFillColor = Color.WHITE
            highlightCircleStrokeColor = STROKE_COLOR
            highlightCircleStrokeWidth = DATASET_HIGHLIGHT_STROKE_WIDTH
            setDrawValues(false)
        }

        // Return radar data
        return RadarData(dataSet)
    }
}