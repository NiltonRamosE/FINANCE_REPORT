package com.nramos.finance_report.ui.auth.login

sealed class LoginEvent {
    object OnGoogleLoginClick : LoginEvent()
}