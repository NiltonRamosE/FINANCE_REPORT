package com.nramos.finance_report.domain.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class User(
    val userId: Long,
    val name: String,
    val email: String,
    val paternalSurname: String? = null,
    val maternalSurname: String? = null,
    val gender: Char? = null
) : Parcelable
