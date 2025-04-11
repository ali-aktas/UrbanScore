package com.aliaktas.urbanscore.ui.auth

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.aliaktas.urbanscore.MainActivity
import com.aliaktas.urbanscore.databinding.FragmentForgotPasswordBinding
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

@AndroidEntryPoint
class ForgotPasswordFragment : Fragment() {

    private var _binding: FragmentForgotPasswordBinding? = null
    private val binding get() = _binding!!
    private var backPressedCallback: OnBackPressedCallback? = null

    @Inject
    lateinit var auth: FirebaseAuth

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentForgotPasswordBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupInputValidation()
        setupClickListeners()
        setupBackNavigation()
    }

    private fun setupInputValidation() {
        binding.etEmail.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val email = s.toString().trim()
                if (email.isNotEmpty() && !isValidEmail(email)) {
                    binding.tilEmail.error = "Invalid email format"
                } else {
                    binding.tilEmail.error = null
                }

                // Enable/disable reset button based on valid input
                binding.btnResetPassword.isEnabled = email.isNotEmpty() && isValidEmail(email)
            }
        })
    }

    private fun isValidEmail(email: String): Boolean {
        return Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    private fun setupClickListeners() {
        // Reset password button
        binding.btnResetPassword.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()
            if (email.isNotEmpty() && isValidEmail(email)) {
                resetPassword(email)
            } else {
                binding.tilEmail.error = if (email.isEmpty()) "Email is required" else "Invalid email format"
            }
        }

        binding.tvBackToLogin.setOnClickListener {
            // handleBackPressed() yerine doğrudan showLoginFragment() çağıralım
            (requireActivity() as MainActivity).showLoginFragment()
        }

        // Back button
        binding.btnBack.setOnClickListener {
            // Mevcut callback'i kaldır (önemli!)
            backPressedCallback?.remove()
            backPressedCallback = null

            // Use MainActivity's custom navigation
            (requireActivity() as MainActivity).handleBackPressed()
        }
    }

    private fun resetPassword(email: String) {
        binding.progressBar.visibility = View.VISIBLE
        binding.btnResetPassword.isEnabled = false

        lifecycleScope.launch {
            try {
                auth.sendPasswordResetEmail(email).await()
                Snackbar.make(
                    binding.root,
                    "Password reset email sent to $email",
                    Snackbar.LENGTH_LONG
                ).show()

                // Use MainActivity's custom navigation
                (requireActivity() as MainActivity).handleBackPressed()
            } catch (e: Exception) {
                val errorMessage = when {
                    e.message?.contains("no user record", ignoreCase = true) == true ->
                        "No account found with this email address"
                    e.message?.contains("invalid email", ignoreCase = true) == true ->
                        "Invalid email address"
                    else -> "Error: ${e.message}"
                }

                Snackbar.make(
                    binding.root,
                    errorMessage,
                    Snackbar.LENGTH_LONG
                ).show()
            } finally {
                binding.progressBar.visibility = View.GONE
                binding.btnResetPassword.isEnabled = true
            }
        }
    }

    private fun setupBackNavigation() {
        // Önceki callback'i temizle
        backPressedCallback?.remove()

        // Yeni callback oluştur - login ekranına dön
        backPressedCallback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                (requireActivity() as MainActivity).showLoginFragment()
            }
        }

        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, backPressedCallback!!)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Callback'i temizle
        backPressedCallback?.remove()
        backPressedCallback = null
        _binding = null
    }
}