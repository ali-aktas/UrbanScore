package com.aliaktas.urbanscore.util

import android.content.Context
import android.view.View
import androidx.fragment.app.Fragment
import com.aliaktas.urbanscore.R
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuthException
import java.net.ConnectException
import java.net.UnknownHostException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ErrorHandler @Inject constructor(
    private val resourceProvider: ResourceProvider
) {

    fun showError(fragment: Fragment, throwable: Throwable, rootView: View? = null) {
        val message = getErrorMessage(throwable)
        showSnackbar(rootView ?: fragment.requireView(), message)
    }

    fun showError(fragment: Fragment, message: String, rootView: View? = null) {
        showSnackbar(rootView ?: fragment.requireView(), message)
    }

    private fun showSnackbar(view: View, message: String) {
        Snackbar.make(view, message, Snackbar.LENGTH_LONG).show()
    }

    fun getErrorMessage(throwable: Throwable): String {
        return when (throwable) {
            is UnknownHostException, is ConnectException ->
                resourceProvider.getString(R.string.error_no_internet)
            is FirebaseNetworkException ->
                resourceProvider.getString(R.string.error_network)
            is FirebaseAuthException ->
                getAuthErrorMessage(throwable)
            else ->
                throwable.message ?: resourceProvider.getString(R.string.error_unknown)
        }
    }

    private fun getAuthErrorMessage(error: FirebaseAuthException): String {
        return when (error.errorCode) {
            "ERROR_INVALID_EMAIL" -> resourceProvider.getString(R.string.error_invalid_email)
            "ERROR_WRONG_PASSWORD" -> resourceProvider.getString(R.string.error_wrong_password)
            "ERROR_USER_NOT_FOUND" -> resourceProvider.getString(R.string.error_user_not_found)
            "ERROR_USER_DISABLED" -> resourceProvider.getString(R.string.error_user_disabled)
            "ERROR_TOO_MANY_REQUESTS" -> resourceProvider.getString(R.string.error_too_many_requests)
            "ERROR_OPERATION_NOT_ALLOWED" -> resourceProvider.getString(R.string.error_operation_not_allowed)
            "ERROR_EMAIL_ALREADY_IN_USE" -> resourceProvider.getString(R.string.error_email_already_in_use)
            "ERROR_WEAK_PASSWORD" -> resourceProvider.getString(R.string.error_weak_password)
            else -> resourceProvider.getString(R.string.error_authentication_failed)
        }
    }
}