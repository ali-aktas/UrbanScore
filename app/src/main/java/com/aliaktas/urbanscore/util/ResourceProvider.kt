package com.aliaktas.urbanscore.util

import android.content.Context
import androidx.annotation.StringRes
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ResourceProvider @Inject constructor(
    private val context: Context
) {
    fun getString(@StringRes resId: Int): String {
        return context.getString(resId)
    }

    fun getString(@StringRes resId: Int, vararg formatArgs: Any): String {
        return context.getString(resId, *formatArgs)
    }

    fun getQuantityString(@StringRes resId: Int, quantity: Int, vararg formatArgs: Any): String {
        return context.resources.getQuantityString(resId, quantity, *formatArgs)
    }
}