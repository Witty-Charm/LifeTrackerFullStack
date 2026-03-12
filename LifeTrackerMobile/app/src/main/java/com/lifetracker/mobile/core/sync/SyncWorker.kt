package com.lifetracker.mobile.core.sync

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import com.lifetracker.mobile.R
import com.lifetracker.mobile.core.network.SafeApiCaller
import com.lifetracker.mobile.core.network.NetworkResult
import com.lifetracker.mobile.data.local.dao.TaskDao
import com.lifetracker.mobile.data.mapper.toDomain
import com.lifetracker.mobile.data.mapper.toEntity
import com.lifetracker.mobile.data.remote.LifeTrackerApi
import com.lifetracker.mobile.data.remote.dto.CreateTaskRequest
import com.lifetracker.mobile.data.remote.dto.TaskType as DtoTaskType
import com.lifetracker.mobile.data.remote.dto.TaskDifficulty as DtoDifficulty
import com.lifetracker.mobile.data.local.entity.TaskEntity
import kotlin.time.Instant
import timber.log.Timber

class SyncWorker(
    appContext: Context,
    workerParams: WorkerParameters,
    private val taskDao: TaskDao,
    private val api: LifeTrackerApi,
    private val caller: SafeApiCaller,
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        const val CHANNEL_ID = "sync_channel"
        const val NOTIFICATION_PROGRESS_ID = 1001
        const val NOTIFICATION_FAILURE_ID  = 1002
    }

    override suspend fun doWork(): Result {
        val pending = taskDao.getPendingSync()
        if (pending.isEmpty()) return Result.success()

        Timber.d("SyncWorker: syncing ${pending.size} pending tasks")

        var anyNetworkError = false
        val failedTitles = mutableListOf<String>()

        for (entity in pending) {
            val request = entity.toCreateRequest()
            val result = caller.safeApiCall { api.createTask(request) }

            when (result) {
                is NetworkResult.Success -> {
                    taskDao.deleteById(entity.id)
                    taskDao.upsert(result.data.toDomain().toEntity(pendingSync = false))
                    Timber.d("SyncWorker: synced task '${entity.title}' → real id ${result.data.id}")
                }

                is NetworkResult.Error -> {
                    Timber.w("SyncWorker: task '${entity.title}' rejected by server (${result.code}), removing")
                    taskDao.deleteById(entity.id)
                    failedTitles.add(entity.title)
                }

                is NetworkResult.Exception -> {
                    Timber.w(result.throwable, "SyncWorker: network error, will retry")
                    anyNetworkError = true
                }
            }
        }

        if (failedTitles.isNotEmpty()) {
            showFailureNotification(failedTitles)
        }

        return if (anyNetworkError) Result.retry() else Result.success()
    }

    override suspend fun getForegroundInfo(): ForegroundInfo {
        ensureChannel()
        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setContentTitle("Tasks synchronizing")
            .setContentText("Send tasks to server...")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setOngoing(true)
            .build()
        return ForegroundInfo(NOTIFICATION_PROGRESS_ID, notification)
    }

    private fun showFailureNotification(titles: List<String>) {
        ensureChannel()
        val text = if (titles.size == 1) {
            "Save task failed: «${titles[0]}»"
        } else {
            "Failed save ${titles.size} task: ${titles.joinToString(", ") { "«$it»" }}"
        }
        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setContentTitle("Sync error")
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setAutoCancel(true)
            .build()

        val manager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE)
            as NotificationManager
        manager.notify(NOTIFICATION_FAILURE_ID, notification)
    }

    private fun ensureChannel() {
        val manager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE)
            as NotificationManager
        if (manager.getNotificationChannel(CHANNEL_ID) != null) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Sync",
            NotificationManager.IMPORTANCE_DEFAULT,
        )
        manager.createNotificationChannel(channel)
    }
}

private fun TaskEntity.toCreateRequest(): CreateTaskRequest = CreateTaskRequest(
    heroId = heroId,
    title = title,
    description = description.ifBlank { null },
    type = when (type) {
        "Habit"   -> DtoTaskType.Habit
        "OneTime" -> DtoTaskType.OneTime
        else      -> DtoTaskType.OneTime
    },
    difficulty = when (difficulty) {
        "Easy"   -> DtoDifficulty.Easy
        "Medium" -> DtoDifficulty.Medium
        "Hard"   -> DtoDifficulty.Hard
        "Epic"   -> DtoDifficulty.Epic
        else     -> DtoDifficulty.Easy
    },
    dueDate = dueDate?.let { Instant.fromEpochMilliseconds(it) },
)
