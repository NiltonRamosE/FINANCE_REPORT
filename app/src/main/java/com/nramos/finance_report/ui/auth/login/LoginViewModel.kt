package com.nramos.finance_report.ui.auth.login

import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nramos.finance_report.data.auth.GoogleSignInManager
import com.nramos.finance_report.domain.usecase.auth.LoginWithGoogleUseCase
import com.nramos.finance_report.utils.NetworkResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val loginWithGoogleUseCase: LoginWithGoogleUseCase,
    private val googleSignInManager: GoogleSignInManager
) : ViewModel() {

    private val _state = MutableStateFlow(LoginState())
    val state: StateFlow<LoginState> = _state.asStateFlow()

    fun onEvent(event: LoginEvent) {
        when (event) {
            LoginEvent.OnGoogleLoginClick -> {
                _state.update { it.copy(isGoogleLoginRequested = true) }
            }
        }
    }

    suspend fun handleGoogleSignInResult(data: Intent?): Boolean {
        _state.update { it.copy(isLoading = true, error = null, isGoogleLoginRequested = false) }

        return try {
            val result = googleSignInManager.handleSignInResult(data)

            if (result.isSuccess) {
                val googleResult = result.getOrNull()!!
                var success = false
                loginWithGoogleUseCase(googleResult).collect { networkResult ->
                    when (networkResult) {
                        is NetworkResult.Loading -> {

                        }
                        is NetworkResult.Success -> {
                            _state.update {
                                it.copy(
                                    isLoading = false,
                                    isSuccess = true,
                                    user = networkResult.data
                                )
                            }
                            success = true
                        }
                        is NetworkResult.Error -> {
                            _state.update {
                                it.copy(
                                    isLoading = false,
                                    error = networkResult.message
                                )
                            }
                        }
                    }
                }
                success
            } else {
                _state.update {
                    it.copy(isLoading = false, error = "Error al iniciar sesión con Google")
                }
                false
            }
        } catch (e: Exception) {
            _state.update {
                it.copy(isLoading = false, error = e.message ?: "Error desconocido")
            }
            false
        }
    }

    fun resetSuccess() {
        _state.update { it.copy(isSuccess = false) }
    }

    fun resetGoogleRequest() {
        _state.update { it.copy(isGoogleLoginRequested = false, isLoading = false) }
    }
}