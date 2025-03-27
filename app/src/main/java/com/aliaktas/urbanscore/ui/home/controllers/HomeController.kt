package com.aliaktas.urbanscore.ui.home.controllers

import android.view.View
import com.aliaktas.urbanscore.databinding.FragmentHomeBinding
import com.aliaktas.urbanscore.ui.home.HomeState

/**
 * HomeFragment için controller arayüzü.
 * Her controller, UI'ın belirli bir bölümünden sorumludur.
 */
interface HomeController {
    /**
     * Controller'ın binding ile ilk kurulumunu yapar
     */
    fun bind(view: View)

    /**
     * Görünüm durumunu günceller
     */
    fun update(state: HomeState)
}