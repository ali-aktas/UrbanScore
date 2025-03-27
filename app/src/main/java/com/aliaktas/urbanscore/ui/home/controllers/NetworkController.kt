package com.aliaktas.urbanscore.ui.home.controllers

import android.util.Log
import android.view.View
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.aliaktas.urbanscore.R
import com.aliaktas.urbanscore.databinding.FragmentHomeBinding
import com.aliaktas.urbanscore.ui.home.HomeState
import com.aliaktas.urbanscore.ui.home.HomeViewModel
import com.aliaktas.urbanscore.util.NetworkUtil
import com.aliaktas.urbanscore.util.ResourceProvider
import kotlinx.coroutines.launch

/**
 * Ağ durumunu yöneten controller
 */
class NetworkController(
    private val binding: FragmentHomeBinding,
    private val networkUtil: NetworkUtil,
    private val viewModel: HomeViewModel,
    private val lifecycleOwner: LifecycleOwner,
    private val mainStateController: MainStateController,
    private val resourceProvider: ResourceProvider
) : HomeController {

    override fun bind(view: View) {
        Log.d(TAG, "Setting up network observation")
        observeNetworkState()
    }

    private fun observeNetworkState() {
        lifecycleOwner.lifecycleScope.launch {
            lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                networkUtil.observeNetworkState().collect { isConnected ->
                    Log.d(TAG, "Network state changed: $isConnected")
                    if (isConnected) {
                        // İnternet bağlantısı var
                        mainStateController.hideErrorUI()
                        viewModel.loadTopRatedCities(true)
                    } else {
                        // İnternet bağlantısı yok
                        val errorMessage = resourceProvider.getString(R.string.no_internet_connection)
                        mainStateController.showErrorUI(errorMessage)
                    }
                }
            }
        }
    }

    /**
     * İnternet bağlantısını kontrol eder ve gerekirse hata UI'ını gösterir
     * @return İnternet bağlantısı varsa true, yoksa false
     */
    fun checkInternetConnection(): Boolean {
        val isConnected = networkUtil.isNetworkAvailable()
        if (!isConnected) {
            val errorMessage = resourceProvider.getString(R.string.no_internet_connection)
            mainStateController.showErrorUI(errorMessage)
        }
        return isConnected
    }

    override fun update(state: HomeState) {
        // Ağ durumu, state'den bağımsız olarak yönetilir
    }

    companion object {
        private const val TAG = "NetworkController"
    }
}