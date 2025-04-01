package com.aliaktas.urbanscore.ui.home.controllers

import android.view.View
import com.aliaktas.urbanscore.databinding.FragmentHomeBinding
import com.aliaktas.urbanscore.ui.home.HomeState

/**
 * HomeFragment için controller arayüzü.
 * Her controller, UI'ın belirli bir bölümünden sorumludur.
 */
interface HomeController {
    fun bind(view: View)
    fun update(state: HomeState)
    // Yeni metot: Bu controller bu state değişimine tepki vermeli mi?
    fun shouldUpdate(state: HomeState): Boolean = true
}