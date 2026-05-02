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
    }

    fun scheduleReminder(reminder: Reminder) {
        if (!reminder.isActive) return

        when (reminder.frequency) {
            "once" -> scheduleOnce(reminder)
            "daily" -> scheduleDaily(reminder)
            "weekly" -> scheduleWeekly(reminder)
            "monthly" -> scheduleMonthly(reminder)
        }
    }

    private fun scheduleOnce(reminder: Reminder) {
        val delay = calculateDelay(reminder.date, reminder.time)
        if (delay <= 0) return

        val inputData = createInputData(reminder)
        val workRequest = OneTimeWorkRequestBuilder<ReminderNotificationWorker>()
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .setInputData(inputData)
            .build()

        workManager.enqueueUniqueWork("reminder_once_${reminder.id}", ExistingWorkPolicy.REPLACE, workRequest)
    }

    private fun scheduleDaily(reminder: Reminder) {
        val inputData = createInputData(reminder)

        // Primera ejecución: calcular delay hasta la próxima hora programada
        val firstDelay = calculateDelayForNextDaily(reminder.time)

        val workRequest = PeriodicWorkRequestBuilder<ReminderNotificationWorker>(
            1, TimeUnit.DAYS  // Se repite cada 1 día
        )
            .setInitialDelay(firstDelay, TimeUnit.MILLISECONDS)
            .setInputData(inputData)
            .build()

        workManager.enqueueUniquePeriodicWork(
            "reminder_daily_${reminder.id}",
            ExistingPeriodicWorkPolicy.REPLACE,
            workRequest
        )
    }

    private fun scheduleWeekly(reminder: Reminder) {
        val inputData = createInputData(reminder)

        val firstDelay = calculateDelayForNextWeekly(reminder.date, reminder.time)

        val workRequest = PeriodicWorkRequestBuilder<ReminderNotificationWorker>(
            7, TimeUnit.DAYS  // Se repite cada 7 días
        )
            .setInitialDelay(firstDelay, TimeUnit.MILLISECONDS)
            .setInputData(inputData)
            .build()

        workManager.enqueueUniquePeriodicWork(
            "reminder_weekly_${reminder.id}",
            ExistingPeriodicWorkPolicy.REPLACE,
            workRequest
        )
    }

    private fun scheduleMonthly(reminder: Reminder) {
        val inputData = createInputData(reminder)
        val delay = calculateDelayForNextMonthly(reminder.date, reminder.time)

        if (delay <= 0) return

        val workRequest = OneTimeWorkRequestBuilder<ReminderNotificationWorker>()
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .setInputData(inputData)
            .build()

        workManager.enqueueUniqueWork(
            "reminder_monthly_${reminder.id}",
            ExistingWorkPolicy.REPLACE,
            workRequest
        )
    }

    private fun calculateDelayForNextDaily(time: String): Long {
        // Calcular delay hasta la próxima ocurrencia de la hora programada
        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        val targetTime = timeFormat.parse(time) ?: return 60000

        val calendar = Calendar.getInstance()
        val now = Date()

        calendar.set(Calendar.HOUR_OF_DAY, targetTime.hours)
        calendar.set(Calendar.MINUTE, targetTime.minutes)
        calendar.set(Calendar.SECOND, 0)

        if (calendar.time <= now) {
            calendar.add(Calendar.DAY_OF_YEAR, 1) // Programar para mañana
        }

        return calendar.time.time - now.time
    }

    private fun calculateDelayForNextWeekly(date: String, time: String): Long {
        // Similar a daily pero para el mismo día de la semana
        return calculateDelayForNextDaily(time)
    }

    private fun calculateDelayForNextMonthly(date: String, time: String): Long {
        return try {
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val reminderDate = dateFormat.parse(date) ?: return 60000

            val calendar = Calendar.getInstance()
            val now = Date()

            // Obtener el día del mes del recordatorio original (ej: 15)
            calendar.time = reminderDate
            val targetDayOfMonth = calendar.get(Calendar.DAY_OF_MONTH)

            // Crear fecha objetivo para este mes
            calendar.time = now
            calendar.set(Calendar.DAY_OF_MONTH, targetDayOfMonth)
            calendar.set(Calendar.HOUR_OF_DAY, getHourFromTime(time))
            calendar.set(Calendar.MINUTE, getMinuteFromTime(time))
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)

            // Si la fecha ya pasó este mes, programar para el próximo mes
            if (calendar.time <= now) {
                calendar.add(Calendar.MONTH, 1)
            }

            // Manejar días inválidos (ej: 31 en febrero)
            val actualDayOfMonth = calendar.get(Calendar.DAY_OF_MONTH)
            if (actualDayOfMonth != targetDayOfMonth) {
                // Programar para el último día del mes
                calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH))
            }

            calendar.time.time - now.time
        } catch (e: Exception) {
            Log.e(TAG, "Error calculando delay mensual: ${e.message}")
            60000
        }
    }

    private fun getHourFromTime(time: String): Int {
        return try {
            val parts = time.split(":")
            parts[0].toInt()
        } catch (e: Exception) {
            9
        }
    }

    private fun getMinuteFromTime(time: String): Int {
        return try {
            val parts = time.split(":")
            parts[1].toInt()
        } catch (e: Exception) {
            0
        }
    }

    private fun createInputData(reminder: Reminder): Data {
        return Data.Builder()
            .putString("reminder_id", reminder.id)
            .putString("reminder_title", reminder.title)
            .putString("reminder_description", reminder.description ?: "")
            .putString("reminder_date", reminder.date)
            .putString("reminder_time", reminder.time)
            .build()
    }

    /**
     * Cancela un recordatorio programado
     */
    fun cancelReminder(reminderId: String) {
        workManager.cancelUniqueWork("reminder_once_$reminderId")
        workManager.cancelUniqueWork("reminder_daily_$reminderId")
        workManager.cancelUniqueWork("reminder_weekly_$reminderId")
        workManager.cancelUniqueWork("reminder_monthly_$reminderId")
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