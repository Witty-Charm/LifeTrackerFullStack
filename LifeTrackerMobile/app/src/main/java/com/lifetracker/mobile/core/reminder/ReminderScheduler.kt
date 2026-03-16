package com.lifetracker.mobile.core.reminder

import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.lifetracker.mobile.domain.model.ReminderItem
import kotlinx.serialization.json.Json
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.concurrent.TimeUnit

class ReminderScheduler(
    private val workManager: WorkManager,
    private val json: Json,
) {

    fun schedule(taskId: Int, taskTitle: String, remindersJson: String, repeatPattern: String) {
        val reminders = parseReminders(remindersJson)
        val intervalHours = parseIntervalHours(repeatPattern)
        reminders.forEach { reminder ->
            val workName = "reminder_${taskId}_${reminder.id}"
            val initialDelay = computeInitialDelay(reminder.hour, reminder.minute)
            val request = PeriodicWorkRequestBuilder<ReminderWorker>(intervalHours, TimeUnit.HOURS)
                .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
                .setInputData(workDataOf("taskId" to taskId, "taskTitle" to taskTitle))
                .addTag("task_$taskId")
                .build()
            workManager.enqueueUniquePeriodicWork(
                workName,
                ExistingPeriodicWorkPolicy.UPDATE,
                request,
            )
        }
    }

    fun cancelAll(taskId: Int) {
        workManager.cancelAllWorkByTag("task_$taskId")
    }

    private fun parseReminders(jsonString: String): List<ReminderItem> =
        json.decodeFromString(jsonString)

    private fun parseIntervalHours(pattern: String): Long {
        val parts = pattern.split(":")
        val interval = parts.getOrNull(1)?.toLongOrNull() ?: 1L
        return when (parts.getOrNull(0)) {
            "DAILY" -> 24L * interval
            "WEEKLY" -> 24L * 7L * interval
            "MONTHLY" -> 24L * 30L * interval
            "YEARLY" -> 24L * 365L * interval
            else -> 24L * interval
        }
    }

    private fun computeInitialDelay(hour: Int, minute: Int): Long {
        val now = LocalDateTime.now()
        var next = now.withHour(hour).withMinute(minute).withSecond(0).withNano(0)
        if (!next.isAfter(now)) {
            next = next.plusDays(1)
        }
        val nowMillis = now.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val nextMillis = next.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        return nextMillis - nowMillis
    }
}
