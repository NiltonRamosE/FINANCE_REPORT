package com.nramos.finance_report.data.model.request

class RegisterRequest {
    val name: String
    val email: String
    val password: String
    val paternalSurname: String?
    val maternalSurname: String?

    var gender: Char? = null

    constructor(
        name: String,
        email: String,
        password: String,
        paternalSurname: String? = null,
        maternalSurname: String? = null,
        gender: Char? = null
    ) {
        this.name = name
        this.email = email
        this.password = password
        this.paternalSurname = paternalSurname
        this.maternalSurname = maternalSurname
        this.gender = gender
    }
}