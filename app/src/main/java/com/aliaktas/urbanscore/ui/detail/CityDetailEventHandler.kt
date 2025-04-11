package com.aliaktas.urbanscore.ui.detail

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.aliaktas.urbanscore.ui.comments.CommentBottomSheet
import com.aliaktas.urbanscore.ui.ratecity.RateCityBottomSheet
import com.google.android.material.snackbar.Snackbar
import javax.inject.Inject
import javax.inject.Singleton

/**
 * CityDetailFragment içindeki olayları yöneten yardımcı sınıf.
 * Fragment'in olay işleme yükünü azaltmak için tasarlanmıştır.
 */
@Singleton
class CityDetailEventHandler @Inject constructor(
    private val context: Context
) {
    private val TAG = "CityDetailEventHandler"

    fun handleEvent(event: CityDetailEvent, fragment: Fragment) {
        when (event) {
            is CityDetailEvent.OpenUrl -> openUrl(event.url, fragment)
            is CityDetailEvent.ShowMessage -> showMessage(event.message, fragment.requireView())
            is CityDetailEvent.ShareCity -> fragment.startActivity(Intent.createChooser(event.shareIntent, "Share via"))
            is CityDetailEvent.ShowRatingSheet -> showRatingBottomSheet(event.cityId, fragment)
            is CityDetailEvent.ShowCommentBottomSheet -> showCommentBottomSheet(event.cityId, fragment)
            else -> {
                // Diğer olayları işle
            }
        }
    }

    private fun openUrl(url: String, fragment: Fragment) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            fragment.startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Error opening URL: ${e.message}", e)
            showMessage("Could not open link: ${e.message}", fragment.requireView())
        }
    }

    private fun showMessage(message: String, view: View) {
        Snackbar.make(view, message, Snackbar.LENGTH_SHORT).show()
    }

    private fun showRatingBottomSheet(cityId: String, fragment: Fragment) {
        try {
            val bottomSheet = RateCityBottomSheet.newInstance(cityId)
            bottomSheet.show(fragment.parentFragmentManager, "RateCityBottomSheet")

            if (fragment is CityDetailFragment) {
                fragment.setRatingBottomSheet(bottomSheet)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error showing rating sheet: ${e.message}", e)
            Toast.makeText(
                fragment.requireContext(),
                "Could not open rating screen: ${e.message}",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun showCommentBottomSheet(cityId: String, fragment: Fragment) {
        val commentBottomSheet = CommentBottomSheet.newInstance(cityId)
        commentBottomSheet.show(fragment.childFragmentManager, "CommentBottomSheet")
    }
}