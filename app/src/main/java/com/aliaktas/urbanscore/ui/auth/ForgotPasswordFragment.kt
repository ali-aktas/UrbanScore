package com.aliaktas.urbanscore.ui.auth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
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

        setupClickListeners()
    }

    private fun setupClickListeners() {
        // Şifre sıfırlama butonu
        binding.btnResetPassword.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()
            if (email.isNotEmpty()) {
                resetPassword(email)
            } else {
                Snackbar.make(binding.root, "Please enter your email", Snackbar.LENGTH_SHORT).show()
            }
        }

        // Giriş sayfasına dön butonu
        binding.tvBackToLogin.setOnClickListener {
            findNavController().navigateUp()
        }

        // Geri butonu
        binding.btnBack.setOnClickListener {
            findNavController().navigateUp()
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
                findNavController().navigateUp()
            } catch (e: Exception) {
                Snackbar.make(
                    binding.root,
                    "Error: ${e.message}",
                    Snackbar.LENGTH_LONG
                ).show()
            } finally {
                binding.progressBar.visibility = View.GONE
                binding.btnResetPassword.isEnabled = true
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}