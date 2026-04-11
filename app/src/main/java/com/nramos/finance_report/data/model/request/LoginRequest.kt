package com.nramos.finance_report.data.model.request

class LoginRequest {
    val email: String
    val password: String

    constructor(email: String, password: String) {
        this.email = email
        this.password = password
    }
}