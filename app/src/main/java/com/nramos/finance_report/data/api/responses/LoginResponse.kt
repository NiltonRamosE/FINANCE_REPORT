package com.nramos.finance_report.data.api.responses

import com.google.gson.annotations.SerializedName

data class LoginResponse(
    @SerializedName("profile_id")
    val profileId: String,
    @SerializedName("name")
    val name: String,
    @SerializedName("email")
    val email: String,
    @SerializedName("token")
    val token: String,
    @SerializedName("token_type")
    val tokenType: String = "Bearer"
)