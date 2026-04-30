package com.nramos.finance_report.domain.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import java.util.Date

@Parcelize
data class Reminder(
    val id: String = "",
    val userId: String,
    val title: String,
    val description: String? = null,
    val dateTime: String,
    val frequency: String, // "once", "daily", "weekly", "monthly", "yearly"
    val isActive: Boolean = true,
) : Parcelable