// com/aliaktas/urbanscore/util/RatingEventBus.kt
package com.aliaktas.urbanscore.util

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

object RatingEventBus {
    // Event tipi için data class (güncellendi)
    data class RatingEvent(val cityId: String, val silentRefresh: Boolean = false)

    // Event yayınlanacak flow
    private val _events = MutableSharedFlow<RatingEvent>()
    val events: SharedFlow<RatingEvent> = _events.asSharedFlow()

    // Event yayınlama fonksiyonu (güncellendi)
    suspend fun emitRatingSubmitted(cityId: String, silentRefresh: Boolean = true) {
        _events.emit(RatingEvent(cityId, silentRefresh))
    }
}