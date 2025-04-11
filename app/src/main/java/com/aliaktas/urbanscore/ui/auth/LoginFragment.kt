package com.aliaktas.urbanscore.ui.auth

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.aliaktas.urbanscore.MainActivity
import com.aliaktas.urbanscore.R
import com.aliaktas.urbanscore.databinding.FragmentLoginBinding
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class LoginFragment : Fragment() {

    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!
    private var backPressedCallback: OnBackPressedCallback? = null

    private val viewModel: AuthViewModel by viewModels()
    private lateinit var googleSignInClient: GoogleSignInClient

    private val googleSignInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(ApiException::class.java)
                account.idToken?.let { idToken ->
                    viewModel.signInWithGoogle(idToken)
                }
            } catch (e: ApiException) {
                showUserFriendlyError("Google sign in failed: ${formatGoogleError(e)}")
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupGoogleSignIn()
        setupClickListeners()
        observeViewModel()
        setupBackNavigation()
    }

    private fun setupBackNavigation() {
        // Önceki callback'i temizle
        backPressedCallback?.remove()

        // Yeni callback oluştur
        backPressedCallback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                // Login ekranından geri tuşuna basınca uygulamadan çık
                requireActivity().finish()
            }
        }

        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, backPressedCallback!!)
    }

    private fun setupGoogleSignIn() {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(requireActivity(), gso)
    }

    private fun setupClickListeners() {
        // Login button
        binding.btnLogin.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString()

            if (email.isNotEmpty() && password.isNotEmpty()) {
                viewModel.signInWithEmail(email, password)
            } else {
                showUserFriendlyError(getString(R.string.fill_all_fields))
            }
        }

        // Create account button
        binding.btnSignUp.setOnClickListener {
            // Use MainActivity's custom navigation instead of Navigation Component
            (requireActivity() as MainActivity).showRegisterFragment()
        }

        // Google sign in button
        binding.btnGoogleSignIn.setOnClickListener {
            val signInIntent = googleSignInClient.signInIntent
            googleSignInLauncher.launch(signInIntent)
        }

        // Forgot password button
        binding.tvForgotPassword.setOnClickListener {
            // Use MainActivity's custom navigation instead of Navigation Component
            (requireActivity() as MainActivity).showForgotPasswordFragment()
        }
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
                binding.btnLogin.isEnabled = false
                binding.btnGoogleSignIn.isEnabled = false
                binding.btnSignUp.isEnabled = false
            }
            is AuthState.Authenticated -> {
                binding.progressBar.visibility = View.GONE
                // Navigate to home screen after successful login
                (requireActivity() as MainActivity).navigateToHomeAfterLogin()
            }
            is AuthState.Unauthenticated -> {
                binding.progressBar.visibility = View.GONE
                binding.btnLogin.isEnabled = true
                binding.btnGoogleSignIn.isEnabled = true
                binding.btnSignUp.isEnabled = true
            }
            is AuthState.Error -> {
                binding.progressBar.visibility = View.GONE
                binding.btnLogin.isEnabled = true
                binding.btnGoogleSignIn.isEnabled = true
                binding.btnSignUp.isEnabled = true

                showUserFriendlyError(state.message)
                viewModel.clearError()
            }
            is AuthState.Initial -> {
                // Initial loading state, do nothing
                binding.progressBar.visibility = View.GONE
            }
        }
    }

    private fun showUserFriendlyError(errorMessage: String) {
        val snackbar = Snackbar.make(binding.root, errorMessage, Snackbar.LENGTH_LONG)

        // Add "Forgot Password" action for password-related errors
        if (errorMessage.contains("password", ignoreCase = true)) {
            snackbar.setAction(getString(R.string.forgot_password_prompt)) {
                (requireActivity() as MainActivity).showForgotPasswordFragment()
            }
        }

        snackbar.show()
    }

    private fun formatGoogleError(e: ApiException): String {
        return when (e.statusCode) {
            7 -> "Network error, check your connection"
            12500 -> "Sign in canceled"
            12501 -> "Sign in error, please try again"
            else -> "Sign in failed"
        }
    }

    override fun onResume() {
        super.onResume()

        // Fragment görünür olduğunda callback'i yeniden ayarla
        setupBackNavigation()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Callback'i temizle
        backPressedCallback?.remove()
        backPressedCallback = null
        _binding = null
    }
}