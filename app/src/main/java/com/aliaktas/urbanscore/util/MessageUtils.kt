package com.aliaktas.urbanscore.util

import android.content.Context
import android.view.View
import android.widget.Toast
import androidx.annotation.StringRes
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar

object MessageUtils {

    // Toast gösterimi için
    fun showToast(context: Context, @StringRes messageResId: Int) {
        Toast.makeText(context, messageResId, Toast.LENGTH_SHORT).show()
    }

    fun showToast(context: Context, message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

    fun showSnackbar(
        view: View,
        @StringRes messageResId: Int,
        duration: Int = Snackbar.LENGTH_LONG,
        @StringRes actionTextResId: Int? = null,
        action: (() -> Unit)? = null
    ) {
        val snackbar = Snackbar.make(view, messageResId, duration)

        if (actionTextResId != null && action != null) {
            snackbar.setAction(actionTextResId) { action() }
        }

        snackbar.show()
    }

    // Dialog gösterimi için
    fun showDialog(
        context: Context,
        @StringRes titleResId: Int,
        @StringRes messageResId: Int,
        @StringRes positiveButtonResId: Int = android.R.string.ok,
        positiveAction: (() -> Unit)? = null,
        @StringRes negativeButtonResId: Int? = null,
        negativeAction: (() -> Unit)? = null
    ) {
        val builder = MaterialAlertDialogBuilder(context)
            .setTitle(titleResId)
            .setMessage(messageResId)
            .setPositiveButton(positiveButtonResId) { dialog, _ ->
                dialog.dismiss()
                positiveAction?.invoke()
            }

        if (negativeButtonResId != null) {
            builder.setNegativeButton(negativeButtonResId) { dialog, _ ->
                dialog.dismiss()
                negativeAction?.invoke()
            }
        }

        builder.show()
    }
}