package com.nramos.finance_report.domain.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class UserProfile(
    val profileId: String,
    val name: String,
    val email: String? = null,
    val paternalSurname: String? = null,
    val maternalSurname: String? = null,
    val gender: Char? = null,
    val avatarUrl: String? = null
) : Parcelable
