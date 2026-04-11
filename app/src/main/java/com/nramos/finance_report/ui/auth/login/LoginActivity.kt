package com.nramos.finance_report.ui.auth.login

import android.content.Intent
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.lifecycleScope
import com.nramos.finance_report.databinding.ActivityLoginBinding
import com.nramos.finance_report.ui.main.MainActivity
import com.nramos.finance_report.utils.showToast
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import com.nramos.finance_report.data.auth.GoogleSignInManager
import javax.inject.Inject

@AndroidEntryPoint
class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private val viewModel: LoginViewModel by viewModels()
    @Inject  // Inyectar GoogleSignInManager
    lateinit var googleSignInManager: GoogleSignInManager

    private val googleSignInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->

        when (result.resultCode) {
            RESULT_OK -> {
                lifecycleScope.launch {
                    val success = viewModel.handleGoogleSignInResult(result.data)
                }
            }
            RESULT_CANCELED -> {
                viewModel.resetGoogleRequest()
                showToast("Inicio de sesión con Google cancelado")
            }
            else -> {
                viewModel.resetGoogleRequest()
                showToast("Error al iniciar sesión con Google (código: ${result.resultCode})")
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupObservers()
        setupListeners()
    }

    private fun setupObservers() {
        lifecycleScope.launch {
            viewModel.state.collectLatest { state ->
                binding.btnLogin.isEnabled = state.isFormValid && !state.isLoading
                binding.btnGoogleLogin.isEnabled = !state.isLoading
                binding.progressBar.visibility = if (state.isLoading) android.view.View.VISIBLE else android.view.View.GONE

                state.error?.let { error ->
                    showToast(error)
                }

                if (state.isSuccess) {
                    showToast("Bienvenido ${state.user?.name}")
                    startActivity(Intent(this@LoginActivity, MainActivity::class.java))
                    finish()
                    viewModel.resetSuccess()
                }
            }
        }
    }

    private fun setupListeners() {
        binding.apply {
            etEmail.addTextChangedListener { text ->
                viewModel.onEvent(LoginEvent.OnEmailChange(text.toString()))
            }

            etPassword.addTextChangedListener { text ->
                viewModel.onEvent(LoginEvent.OnPasswordChange(text.toString()))
            }

            btnLogin.setOnClickListener {
                viewModel.onEvent(LoginEvent.OnLoginClick)
            }

            tvRegister.setOnClickListener {

            }

            btnGoogleLogin.setOnClickListener {
                viewModel.onEvent(LoginEvent.OnGoogleLoginClick)
                val signInIntent = googleSignInManager.getSignInIntent()
                googleSignInLauncher.launch(signInIntent)
            }
        }
    }
}