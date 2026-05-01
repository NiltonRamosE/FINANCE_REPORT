// worker/ReminderNotificationWorker.kt
package com.nramos.finance_report.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.nramos.finance_report.R
import com.nramos.finance_report.ui.main.MainActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ReminderNotificationWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            // Obtener datos del recordatorio desde los parámetros
            val reminderId = inputData.getString("reminder_id") ?: return@withContext Result.failure()
            val reminderTitle = inputData.getString("reminder_title") ?: "Recordatorio"
            val reminderDescription = inputData.getString("reminder_description") ?: ""
            val reminderDate = inputData.getString("reminder_date") ?: ""
            val reminderTime = inputData.getString("reminder_time") ?: ""

            // Mostrar notificación
            showNotification(reminderTitle, reminderDescription, reminderId)

            Result.success()
        } catch (e: Exception) {
            Result.failure()
        }
    }

    private fun showNotification(title: String, message: String, reminderId: String) {
        // Crear canal de notificación (Android 8+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "reminder_channel",
                "Recordatorios",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Canal para recordatorios programados"
                enableVibration(true)
                vibrationPattern = longArrayOf(100, 200, 100, 200)
            }
            val manager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }

        // Intent para abrir la app al hacer clic
        val intent = Intent(applicationContext, MainActivity::class.java).apply {
            putExtra("reminder_id", reminderId)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            reminderId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Construir notificación
        val notification = NotificationCompat.Builder(applicationContext, "reminder_channel")
            .setContentTitle(title)
            .setContentText(message)
            .setSmallIcon(R.drawable.ic_notifications)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        // Mostrar notificación
        val manager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(reminderId.hashCode(), notification)
    }
}