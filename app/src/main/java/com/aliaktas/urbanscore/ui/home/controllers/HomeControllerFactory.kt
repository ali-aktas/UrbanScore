package com.aliaktas.urbanscore.ui.home.controllers

import androidx.lifecycle.LifecycleOwner
import com.aliaktas.urbanscore.databinding.FragmentHomeBinding
import com.aliaktas.urbanscore.data.model.CategoryModel
import com.aliaktas.urbanscore.ui.home.HomeViewModel
import com.aliaktas.urbanscore.util.ImageLoader
import com.aliaktas.urbanscore.util.NetworkUtil
import com.aliaktas.urbanscore.util.ResourceProvider
import javax.inject.Inject
import javax.inject.Singleton

/**
 * HomeFragment için controller'ları oluşturan factory sınıfı.
 */
@Singleton
class HomeControllerFactory @Inject constructor(
    private val networkUtil: NetworkUtil,
    private val resourceProvider: ResourceProvider,
    private val imageLoader: ImageLoader // ImageLoader'ı ekledik
) {
    /**
     * Fragment için tüm controller'ları oluşturur
     */
    fun createControllers(
        binding: FragmentHomeBinding,
        viewModel: HomeViewModel,
        lifecycleOwner: LifecycleOwner,
        onCityClick: (String) -> Unit,
        onCategoryClick: (CategoryModel) -> Unit,
        onViewAllClick: () -> Unit,
        onRetry: () -> Unit
    ): List<HomeController> {
        // Önce MainStateController'ı oluştur
        val mainStateController = MainStateController(binding, onRetry)

        // Sonra NetworkController'ı oluştur
        val networkController = NetworkController(
            binding,
            networkUtil,
            viewModel,
            lifecycleOwner,
            mainStateController,
            resourceProvider
        )

        // Diğer controller'lar için internet kontrolü işlevi
        val checkInternetBeforeClick: () -> Boolean = {
            networkController.checkInternetConnection()
        }

        // Ardından SwipeRefreshController'ı oluştur
        val swipeRefreshController = SwipeRefreshController(
            binding,
            viewModel,
            networkController
        )

        // Diğer controller'ları oluştur
        return listOf(
            mainStateController,
            networkController,
            swipeRefreshController,
            CategoriesController(binding, onCategoryClick, checkInternetBeforeClick),
            TopCitiesController(binding, onCityClick, onViewAllClick, checkInternetBeforeClick, imageLoader), // ImageLoader ekledik
            EditorsChoiceController(binding, viewModel, lifecycleOwner, onCityClick, checkInternetBeforeClick)
        )
    }
}