package com.aliaktas.urbanscore.ui.auth

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.aliaktas.urbanscore.R
import com.aliaktas.urbanscore.databinding.FragmentRegisterBinding
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class RegisterFragment : Fragment() {

    private var _binding: FragmentRegisterBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AuthViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRegisterBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupClickListeners()
        observeViewModel()

        // Hata izleme için
        Thread.setDefaultUncaughtExceptionHandler { _, throwable ->
            Log.e("RegisterFragment", "Uncaught exception: ${throwable.message}", throwable)
        }
    }

    private fun setupClickListeners() {
        // Kayıt ol butonu
        binding.btnRegister.setOnClickListener {
            val name = binding.etName.text.toString().trim()
            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString()
            val confirmPassword = binding.etConfirmPassword.text.toString()

            if (validateInput(name, email, password, confirmPassword)) {
                viewModel.signUpWithEmail(email, password, name)
            }
        }

        // Giriş yap butonu
        binding.tvLogin.setOnClickListener {
            findNavController().navigateUp()
        }

        // Geri butonu
        binding.btnBack.setOnClickListener {
            findNavController().navigateUp()
        }
    }

    private fun validateInput(name: String, email: String, password: String, confirmPassword: String): Boolean {
        if (name.isEmpty() || email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
            Snackbar.make(binding.root, "Please fill all fields", Snackbar.LENGTH_SHORT).show()
            return false
        }

        if (password.length < 6) {
            Snackbar.make(binding.root, "Password must be at least 6 characters", Snackbar.LENGTH_SHORT).show()
            return false
        }

        if (password != confirmPassword) {
            Snackbar.make(binding.root, "Passwords don't match", Snackbar.LENGTH_SHORT).show()
            return false
        }

        if (!binding.cbTerms.isChecked) {
            Snackbar.make(binding.root, "Please agree to the terms and privacy policy", Snackbar.LENGTH_SHORT).show()
            return false
        }

        return true
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.state.collect { state ->
                    updateUI(state)
                }
            }
        }
    }

    private fun updateUI(state: AuthState) {
        when (state) {
            is AuthState.Loading -> {
                binding.progressBar.visibility = View.VISIBLE
                binding.btnRegister.isEnabled = false
            }
            is AuthState.Authenticated -> {
                binding.progressBar.visibility = View.GONE
                // Ana sayfaya yönlendir
                findNavController().navigate(R.id.action_registerFragment_to_homeFragment)
            }
            is AuthState.Unauthenticated -> {
                binding.progressBar.visibility = View.GONE
                binding.btnRegister.isEnabled = true
            }
            is AuthState.Error -> {
                binding.progressBar.visibility = View.GONE
                binding.btnRegister.isEnabled = true
                Snackbar.make(binding.root, state.message, Snackbar.LENGTH_LONG).show()
                viewModel.clearError()
            }
            is AuthState.Initial -> {
                // İlk yükleme durumu, bir şey yapma
                binding.progressBar.visibility = View.GONE
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}