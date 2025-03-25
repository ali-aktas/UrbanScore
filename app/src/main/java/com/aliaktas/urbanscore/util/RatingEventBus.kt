package com.aliaktas.urbanscore.util

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

object RatingEventBus {
    // Event tipi için data class
    data class RatingEvent(val cityId: String)

    // Event yayınlanacak flow
    private val _events = MutableSharedFlow<RatingEvent>()
    val events: SharedFlow<RatingEvent> = _events.asSharedFlow()

    // Event yayınlama fonksiyonu
    suspend fun emitRatingSubmitted(cityId: String) {
        _events.emit(RatingEvent(cityId))
    }
}