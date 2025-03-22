// com/aliaktas/urbanscore/ui/detail/CityDetailUiStateManager.kt
package com.aliaktas.urbanscore.ui.detail

import android.content.Context
import androidx.lifecycle.LifecycleCoroutineScope
import com.aliaktas.urbanscore.MainActivity
import com.aliaktas.urbanscore.databinding.FragmentCityDetailBinding
import com.aliaktas.urbanscore.ui.detail.controllers.ActionButtonsController
import com.aliaktas.urbanscore.ui.detail.controllers.BasicInfoController
import com.aliaktas.urbanscore.ui.detail.controllers.CommentsController
import com.aliaktas.urbanscore.ui.detail.controllers.ExploreButtonsController
import com.aliaktas.urbanscore.ui.detail.controllers.MainStateController
import com.aliaktas.urbanscore.ui.detail.controllers.RadarChartController
import com.aliaktas.urbanscore.ui.detail.controllers.RatingCategoriesController
import com.aliaktas.urbanscore.ui.detail.controllers.UiController
import javax.inject.Inject

/**
 * DEPRECATED - Bu sınıf artık controller mimarisi ile değiştirilmiştir.
 *
 * Geriye dönük uyumluluk için korunmuştur, ancak yeni CityDetailFragment yapısında
 * controller mimarisi kullanmanız önerilir.
 */
@Deprecated(
    message = "Use controller architecture instead",
    replaceWith = ReplaceWith("CityDetailUiControllerFactory")
)
class CityDetailUiStateManager(
    private val context: Context,
    private val binding: FragmentCityDetailBinding,
    private val lifecycleScope: LifecycleCoroutineScope,
    private val formatter: CityDetailFormatter,
    private val radarChartHelper: RadarChartHelper,
    private val onRetry: () -> Unit,
    private val onGoBack: () -> Unit,
    private val onRateButtonClick: () -> Unit
) {
    private val controllers = mutableListOf<UiController>()

    init {
        // Initialize controllers as a compatibility layer
        val mainStateController = MainStateController(binding, onRetry, onGoBack)
        val basicInfoController = BasicInfoController(binding, formatter)
        val radarChartController = RadarChartController(binding, radarChartHelper, object : androidx.lifecycle.LifecycleOwner {
            override val lifecycle: androidx.lifecycle.Lifecycle
                get() = androidx.lifecycle.LifecycleRegistry(this)
        })

        // Add controllers to list
        controllers.add(mainStateController)
        controllers.add(basicInfoController)
        controllers.add(radarChartController)

        // Bind controllers
        controllers.forEach { it.bind(binding.root) }
    }

    /**
     * UI durumunu günceller.
     * Bu metot artık tüm controller'ları topluca günceller.
     */
    fun updateUI(state: CityDetailState) {
        controllers.forEach { it.update(state) }
    }
}