package com.aliaktas.urbanscore.ui.detail.controllers

import android.view.View
import com.aliaktas.urbanscore.ui.detail.CityDetailState

/**
 * UI bileşenlerini kontrol etmek için temel arayüz.
 * Her controller belirli bir UI bölümünden sorumludur.
 */
interface UiController {
    /**
     * Controller'ı başlatır ve ilk UI bağlamalarını yapar
     */
    fun bind(view: View)

    /**
     * Değişen state'e göre UI'ı günceller
     */
    fun update(state: CityDetailState)
}