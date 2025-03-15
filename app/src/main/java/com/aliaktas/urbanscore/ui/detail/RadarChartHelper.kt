package com.aliaktas.urbanscore.ui.detail

import android.content.Context
import android.graphics.Color
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

/**
 * Helper class to manage radar chart operations.
 * Handles chart setup, data processing and updates.
 */
class RadarChartHelper(private val context: Context) {

    /**
     * Setup the radar chart with initial configuration.
     * This should be called once when the chart is first initialized.
     */
    suspend fun setupRadarChart(chart: RadarChart) = withContext(Dispatchers.Main) {
        chart.apply {
            // Chart configuration
            description.isEnabled = false
            webLineWidth = 0.7f
            webColor = Color.parseColor("#33FFFFFF")
            webLineWidthInner = 0.5f
            webColorInner = Color.parseColor("#22FFFFFF")
            webAlpha = 70

            // Layout configuration
            minOffset = 65f
            setExtraOffsets(0f, 0f, 0f, 0f)

            // X axis labels
            xAxis.apply {
                textSize = 12f
                textColor = Color.parseColor("#1BA4C6")
                yOffset = 1f
                xOffset = 0f
                typeface = context.resources.getFont(R.font.poppins_medium)
            }

            // Y axis (values axis)
            yAxis.apply {
                setLabelCount(6, true)
                textColor = Color.parseColor("#1BA4C6")
                textSize = 9f
                axisMinimum = 0f
                axisMaximum = 10f
                setDrawLabels(false)
                typeface = context.resources.getFont(R.font.poppins_medium)
            }

            // Legend
            legend.isEnabled = false
        }
    }

    /**
     * Process city ratings and update chart data.
     * This can be called whenever the data changes.
     * Processing happens on background thread, UI update on main thread.
     */
    suspend fun updateChartData(chart: RadarChart, ratings: CategoryRatings) {
        // Calculate chart data on IO thread
        val chartData = withContext(Dispatchers.Default) {
            createChartData(ratings)
        }

        // Update UI on Main thread
        withContext(Dispatchers.Main) {
            // Set category labels
            chart.xAxis.valueFormatter = IndexAxisValueFormatter(CATEGORIES)

            // Apply chart data and animate
            chart.data = chartData
            chart.invalidate()
            chart.animateXY(1500, 1500)
        }
    }

    /**
     * Create radar chart data from category ratings.
     * This is a CPU-intensive operation, so it runs on background thread.
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
        val primaryColor = Color.parseColor("#DF21F398")
        val strokeColor = Color.parseColor("#1BA4C6")

        val dataSet = RadarDataSet(entries, "").apply {
            color = strokeColor
            fillColor = primaryColor
            setDrawFilled(true)
            fillAlpha = 110
            lineWidth = 1.6f
            valueTextColor = Color.WHITE
            valueTextSize = 12f
            isDrawHighlightCircleEnabled = true
            setDrawHighlightIndicators(false)
            highlightCircleFillColor = Color.WHITE
            highlightCircleStrokeColor = strokeColor
            highlightCircleStrokeWidth = 1f
            setDrawValues(false)
        }

        // Return radar data
        return RadarData(dataSet)
    }

    companion object {
        private val CATEGORIES = arrayOf("View", "Safety", "Livability", "Cost", "Social")
    }
}