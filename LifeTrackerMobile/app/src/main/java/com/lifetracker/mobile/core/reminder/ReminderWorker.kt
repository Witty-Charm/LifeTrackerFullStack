package com.lifetracker.mobile.core.reminder

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.lifetracker.mobile.R

class ReminderWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val taskId = inputData.getInt("taskId", -1)
        val taskTitle = inputData.getString("taskTitle") ?: return Result.failure()
        showNotification(applicationContext, taskId, taskTitle)
        return Result.success()
    }

    private fun showNotification(context: Context, taskId: Int, title: String) {
        val manager = context.getSystemService(NotificationManager::class.java)
        val channelId = "daily_reminders"
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Daily Reminders",
                NotificationManager.IMPORTANCE_DEFAULT,
            )
            manager.createNotificationChannel(channel)
        }
        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("LifeTracker")
            .setContentText("Time for: $title")
            .setAutoCancel(true)
            .build()
        val notificationId = (taskId * 1000) + notification.hashCode()
        manager.notify(notificationId, notification)
    }
}
