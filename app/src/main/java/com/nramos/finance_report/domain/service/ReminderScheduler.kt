package com.nramos.finance_report.domain.service

import android.content.Context
import android.util.Log
import androidx.work.*
import com.nramos.finance_report.domain.model.Reminder
import com.nramos.finance_report.worker.ReminderNotificationWorker
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

class ReminderScheduler(private val context: Context) {

    private val workManager = WorkManager.getInstance(context)
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())

    companion object {
        private const val TAG = "ReminderScheduler"
        private const val WORK_TAG_PREFIX = "reminder_work_"
    }

    /**
     * Programa un recordatorio en la fecha y hora especificadas
     */
    fun scheduleReminder(reminder: Reminder) {
        if (!reminder.isActive) {
            Log.d(TAG, "Recordatorio inactivo: ${reminder.id}")
            return
        }

        val delay = calculateDelay(reminder.date, reminder.time)
        if (delay <= 0) {
            Log.d(TAG, "La fecha ya pasó, no se programa: ${reminder.date} ${reminder.time}")
            return
        }

        Log.d(TAG, "Programando recordatorio: ${reminder.title}")
        Log.d(TAG, "Fecha/Hora: ${reminder.date} ${reminder.time}")
        Log.d(TAG, "Delay: ${delay}ms (${delay / 1000 / 60} minutos)")

        val inputData = Data.Builder()
            .putString("reminder_id", reminder.id)
            .putString("reminder_title", reminder.title)
            .putString("reminder_description", reminder.description ?: "")
            .putString("reminder_date", reminder.date)
            .putString("reminder_time", reminder.time)
            .build()

        val workRequest = OneTimeWorkRequestBuilder<ReminderNotificationWorker>()
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .setInputData(inputData)
            .build()

        workManager.enqueueUniqueWork(
            "$WORK_TAG_PREFIX${reminder.id}",
            ExistingWorkPolicy.REPLACE,
            workRequest
        )

        Log.d(TAG, "Recordatorio programado exitosamente")
    }

    /**
     * Cancela un recordatorio programado
     */
    fun cancelReminder(reminderId: String) {
        workManager.cancelUniqueWork("$WORK_TAG_PREFIX$reminderId")
        Log.d(TAG, "Recordatorio cancelado: $reminderId")
    }

    /**
     * Cancela todos los recordatorios programados
     */
    fun cancelAllReminders() {
        workManager.cancelAllWork()
        Log.d(TAG, "Todos los recordatorios cancelados")
    }

    /**
     * Calcula el delay en milisegundos hasta la fecha/hora objetivo
     */
    private fun calculateDelay(date: String, time: String): Long {
        return try {
            val dateTimeString = "$date $time"
            val targetDate = dateFormat.parse(dateTimeString) ?: return 0
            val now = Date()

            val delay = targetDate.time - now.time
            if (delay < 0) 0 else delay
        } catch (e: Exception) {
            Log.e(TAG, "Error calculando delay: ${e.message}")
            0
        }
    }
}