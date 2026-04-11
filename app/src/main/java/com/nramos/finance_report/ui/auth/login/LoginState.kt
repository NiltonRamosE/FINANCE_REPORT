package com.nramos.finance_report.ui.auth.login

import com.nramos.finance_report.domain.model.UserProfile

data class LoginState(
    val email: String = "",
    val password: String = "",
    val isFormValid: Boolean = false,
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false,
    val error: String? = null,
    val user: UserProfile? = null,
    val isGoogleLoginRequested: Boolean = false
)

sealed class LoginEvent {
    data class OnEmailChange(val email: String) : LoginEvent()
    data class OnPasswordChange(val password: String) : LoginEvent()
    object OnLoginClick : LoginEvent()

    object OnGoogleLoginClick : LoginEvent()
}