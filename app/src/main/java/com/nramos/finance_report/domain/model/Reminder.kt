package com.nramos.finance_report.domain.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Reminder(
    val id: String = "",
    val userId: String,
    val title: String,
    val description: String? = null,
    val date: String,
    val time: String,
    val frequency: String, // "once", "daily", "weekly", "monthly", "yearly"
    val isActive: Boolean = true,
) : Parcelable