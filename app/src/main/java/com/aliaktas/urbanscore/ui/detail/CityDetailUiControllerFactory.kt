// com/aliaktas/urbanscore/ui/detail/CityDetailUiControllerFactory.kt
package com.aliaktas.urbanscore.ui.detail

import androidx.lifecycle.LifecycleOwner
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
import javax.inject.Singleton

// CityDetailUiControllerFactory.kt
@Singleton
class CityDetailUiControllerFactory @Inject constructor(
    private val formatter: CityDetailFormatter,
    private val radarChartHelper: RadarChartHelper
) {

    fun createControllers(
        binding: FragmentCityDetailBinding,
        lifecycleOwner: LifecycleOwner,
        viewModel: CityDetailViewModel,
        fragment: CityDetailFragment  // Fragment referansını ekledik
    ): List<UiController> {
        return listOf(
            MainStateController(
                binding,
                viewModel::loadCityDetails,
                fragment::navigateBack  // Güvenli navigasyon metodunu kullan
            ),
            BasicInfoController(binding, formatter),
            RadarChartController(binding, radarChartHelper, lifecycleOwner),
            ActionButtonsController(binding, viewModel),
            RatingCategoriesController(binding, formatter),
            CommentsController(binding, viewModel, lifecycleOwner),
            ExploreButtonsController(binding, viewModel)
        )
    }
}