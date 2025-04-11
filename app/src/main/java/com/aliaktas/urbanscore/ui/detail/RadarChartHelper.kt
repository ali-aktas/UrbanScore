package com.aliaktas.urbanscore.ui.detail

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.util.Log
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
        private const val TAG = "RadarChartHelper"
        private val CATEGORIES = arrayOf("Aesth.", "Safety", "Livab.", "Gastr.", "Cult.", "Hosp.", "Soc.")


        @ColorInt private val PRIMARY_COLOR = Color.parseColor("#1e202c")
        @ColorInt private val STROKE_COLOR = Color.parseColor("#60519b")
        @ColorInt private val TEXT_COLOR = Color.parseColor("#bfc0d1")
        @ColorInt private val WEB_COLOR = Color.parseColor("#bfc0d1")
        @ColorInt private val WEB_COLOR_INNER = Color.parseColor("#bfc0d1")

        private const val WEB_LINE_WIDTH = 0.7f
        private const val WEB_LINE_WIDTH_INNER = 0.5f
        private const val WEB_ALPHA = 100

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
    suspend fun setupRadarChart(chart: RadarChart) = withContext(Dispatchers.Default) {
        // Chart konfigürasyonlarını arka planda hazırla
        val config = prepareChartConfig()

        // UI güncellemesini ana thread'de yap
        withContext(Dispatchers.Main) {
            applyChartConfig(chart, config)
        }
    }

    // Chart konfigürasyonunu hazırla (arka plan thread'inde çalışacak)
    private fun prepareChartConfig(): ChartConfig {
        return ChartConfig(
            webLineWidth = WEB_LINE_WIDTH,
            webColor = WEB_COLOR,
            webLineWidthInner = WEB_LINE_WIDTH_INNER,
            webColorInner = WEB_COLOR_INNER,
            webAlpha = WEB_ALPHA,
            minOffset = CHART_MIN_OFFSET,
            axisTextSize = AXIS_TEXT_SIZE,
            axisValueTextSize = AXIS_VALUE_TEXT_SIZE,
            axisTextColor = TEXT_COLOR,
            axisMin = AXIS_MIN,
            axisMax = AXIS_MAX,
            categories = CATEGORIES
        )
    }

    // Hazırlanan konfigürasyonu chart'a uygula (ana thread'de çalışacak)
    @SuppressLint("NewApi")
    private fun applyChartConfig(chart: RadarChart, config: ChartConfig) {
        chart.apply {
            // Chart configuration
            description.isEnabled = false
            webLineWidth = config.webLineWidth
            webColor = config.webColor
            webLineWidthInner = config.webLineWidthInner
            webColorInner = config.webColorInner
            webAlpha = config.webAlpha

            // Layout configuration
            minOffset = config.minOffset
            setExtraOffsets(0f, 0f, 0f, 0f)

            // X axis labels
            xAxis.apply {
                textSize = config.axisTextSize
                textColor = config.axisTextColor
                yOffset = 1f
                xOffset = 0f
                typeface = context.resources.getFont(R.font.poppins_medium)
                valueFormatter = IndexAxisValueFormatter(config.categories)
            }

            // Y axis (values axis)
            yAxis.apply {
                setLabelCount(6, true)
                textColor = config.axisTextColor
                textSize = config.axisValueTextSize
                axisMinimum = config.axisMin
                axisMaximum = config.axisMax
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
        // Eğer grafiğin hali hazırda verileri varsa ve güncellenmesi gerekmiyorsa atlayalım
        if (chart.data != null && !hasRatingsChanged(chart.data as RadarData, ratings)) {
            Log.d(TAG, "Radar chart verileri değişmemiş, güncelleme atlanıyor")
            return
        }

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

    // Radar verilerinin değişip değişmediğini kontrol eden yardımcı metod
    private fun hasRatingsChanged(existingData: RadarData, newRatings: CategoryRatings): Boolean {
        if (existingData.dataSetCount == 0) return true

        val dataSet = existingData.getDataSetByIndex(0) as? RadarDataSet ?: return true
        if (dataSet.entryCount < 5) return true

        // Değerleri tek tek karşılaştır
        return dataSet.getEntryForIndex(0).value != newRatings.aesthetics.toFloat() ||
                dataSet.getEntryForIndex(1).value != newRatings.safety.toFloat() ||
                dataSet.getEntryForIndex(2).value != newRatings.livability.toFloat() ||
                dataSet.getEntryForIndex(3).value != newRatings.gastronomy.toFloat() ||
                dataSet.getEntryForIndex(3).value != newRatings.culture.toFloat() ||
                dataSet.getEntryForIndex(3).value != newRatings.hospitality.toFloat() ||
                dataSet.getEntryForIndex(4).value != newRatings.social.toFloat()
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
            add(RadarEntry(ratings.aesthetics.toFloat()))
            add(RadarEntry(ratings.safety.toFloat()))
            add(RadarEntry(ratings.livability.toFloat()))
            add(RadarEntry(ratings.gastronomy.toFloat()))
            add(RadarEntry(ratings.culture.toFloat()))
            add(RadarEntry(ratings.hospitality.toFloat()))
            add(RadarEntry(ratings.social.toFloat()))
        }

        val myColor = Color.parseColor("#FFC107")
        // Create and style data set
        val dataSet = RadarDataSet(entries, "").apply {
            color = myColor        // çizgi rengi
            fillColor = myColor    // iç dolgu rengi
            fillAlpha = 174
            lineWidth = DATASET_LINE_WIDTH
            setDrawFilled(true)
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

    // Chart konfigürasyonu için veri sınıfı
    private data class ChartConfig(
        val webLineWidth: Float,
        val webColor: Int,
        val webLineWidthInner: Float,
        val webColorInner: Int,
        val webAlpha: Int,
        val minOffset: Float,
        val axisTextSize: Float,
        val axisValueTextSize: Float,
        val axisTextColor: Int,
        val axisMin: Float,
        val axisMax: Float,
        val categories: Array<String>
    )
}