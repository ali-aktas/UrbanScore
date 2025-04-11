package com.aliaktas.urbanscore.ui.auth

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.aliaktas.urbanscore.MainActivity
import com.aliaktas.urbanscore.R
import com.aliaktas.urbanscore.databinding.FragmentRegisterBinding
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class RegisterFragment : Fragment() {

    private var _binding: FragmentRegisterBinding? = null
    private val binding get() = _binding!!
    private var backPressedCallback: OnBackPressedCallback? = null
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
        setupInputValidation()
        observeViewModel()
        setupBackNavigation()
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

    private fun setupClickListeners() {
        // Register button
        binding.btnSignUp.setOnClickListener {
            val name = binding.etFullName.text.toString().trim()
            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString()
            val confirmPassword = binding.etConfirmPassword.text.toString()

            if (validateInput(name, email, password, confirmPassword)) {
                viewModel.signUpWithEmail(email, password, name)
            }
        }

        binding.tvBackToLogin.setOnClickListener {
            // handleBackPressed() yerine doğrudan showLoginFragment() çağıralım
            (requireActivity() as MainActivity).showLoginFragment()
        }

        // Google Sign In button
        binding.btnGoogleSignIn.setOnClickListener {
            (requireActivity() as MainActivity).showLoginFragment()
        }
    }

    private fun setupInputValidation() {
        // Name validation
        binding.etFullName.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                binding.tilFullName.error = if (s.toString().trim().isEmpty()) "Name is required" else null
            }
        })

        // Email validation
        binding.etEmail.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val email = s.toString().trim()
                binding.tilEmail.error = when {
                    email.isEmpty() -> "Email is required"
                    !isValidEmail(email) -> "Invalid email format"
                    else -> null
                }
            }
        })

        // Phone validation (optional)
        binding.etPhone.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                // Phone is optional so no validation needed
            }
        })

        // Password validation
        binding.etPassword.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val password = s.toString()
                binding.tilPassword.error = when {
                    password.isEmpty() -> "Password is required"
                    password.length < 6 -> getString(R.string.password_too_short)
                    else -> null
                }

                // Check if confirm password matches
                val confirmPassword = binding.etConfirmPassword.text.toString()
                if (confirmPassword.isNotEmpty() && confirmPassword != password) {
                    binding.tilConfirmPassword.error = getString(R.string.passwords_dont_match)
                } else if (confirmPassword.isNotEmpty()) {
                    binding.tilConfirmPassword.error = null
                }
            }
        })

        // Confirm password validation
        binding.etConfirmPassword.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val confirmPassword = s.toString()
                val password = binding.etPassword.text.toString()

                binding.tilConfirmPassword.error = when {
                    confirmPassword.isEmpty() -> "Confirm password is required"
                    confirmPassword != password -> getString(R.string.passwords_dont_match)
                    else -> null
                }
            }
        })
    }

    private fun validateInput(name: String, email: String, password: String, confirmPassword: String): Boolean {
        var isValid = true

        if (name.isEmpty()) {
            binding.tilFullName.error = "Name is required"
            isValid = false
        }

        if (email.isEmpty()) {
            binding.tilEmail.error = "Email is required"
            isValid = false
        } else if (!isValidEmail(email)) {
            binding.tilEmail.error = "Invalid email format"
            isValid = false
        }

        if (password.isEmpty()) {
            binding.tilPassword.error = "Password is required"
            isValid = false
        } else if (password.length < 6) {
            binding.tilPassword.error = getString(R.string.password_too_short)
            isValid = false
        }

        if (confirmPassword.isEmpty()) {
            binding.tilConfirmPassword.error = "Confirm password is required"
            isValid = false
        } else if (password != confirmPassword) {
            binding.tilConfirmPassword.error = getString(R.string.passwords_dont_match)
            isValid = false
        }

        if (!binding.cbTerms.isChecked) {
            Snackbar.make(binding.root, getString(R.string.please_agree_terms), Snackbar.LENGTH_SHORT).show()
            isValid = false
        }

        return isValid
    }

    private fun isValidEmail(email: String): Boolean {
        return Patterns.EMAIL_ADDRESS.matcher(email).matches()
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
                binding.btnSignUp.isEnabled = false
            }
            is AuthState.Authenticated -> {
                binding.progressBar.visibility = View.GONE
                // Navigate to home screen
                try {
                    (requireActivity() as MainActivity).navigateToHomeAfterLogin()
                } catch (e: Exception) {
                    Log.e("RegisterFragment", "Navigation error", e)
                }
            }
            is AuthState.Unauthenticated -> {
                binding.progressBar.visibility = View.GONE
                binding.btnSignUp.isEnabled = true
            }
            is AuthState.Error -> {
                binding.progressBar.visibility = View.GONE
                binding.btnSignUp.isEnabled = true
                Snackbar.make(binding.root, state.message, Snackbar.LENGTH_LONG).show()
                viewModel.clearError()
            }
            is AuthState.Initial -> {
                // Initial loading state, do nothing
                binding.progressBar.visibility = View.GONE
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Callback'i temizle
        backPressedCallback?.remove()
        backPressedCallback = null
        _binding = null
    }
}