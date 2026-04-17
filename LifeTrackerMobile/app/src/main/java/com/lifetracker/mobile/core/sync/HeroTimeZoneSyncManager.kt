package com.lifetracker.mobile.core.sync

import com.lifetracker.mobile.domain.model.DomainResult
import com.lifetracker.mobile.domain.model.HeroDomain
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.datetime.TimeZone
import timber.log.Timber

class HeroTimeZoneSyncManager(
    private val getFirstHero: suspend () -> DomainResult<HeroDomain?>,
    private val updateHeroTimeZone: suspend (heroId: Int, timeZoneId: String) -> DomainResult<Unit>,
    private val timeZoneProvider: () -> String = { TimeZone.currentSystemDefault().id },
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) {
    private val mutex = Mutex()
    private val scope = CoroutineScope(SupervisorJob() + ioDispatcher)

    @Volatile
    private var lastSyncedHeroTimeZone: Pair<Int, String>? = null

    fun syncIfNeededAsync() {
        scope.launch {
            syncIfNeeded()
        }
    }

    suspend fun syncIfNeeded() {
        withContext(ioDispatcher) {
            val timeZoneId = currentTimeZoneId()

            mutex.withLock {
                val heroResult = getFirstHero()
                val hero = when (heroResult) {
                    is DomainResult.Success -> heroResult.data
                    is DomainResult.Failure -> {
                        Timber.w("Hero timezone sync skipped: cannot load hero (%s)", heroResult.error)
                        null
                    }
                } ?: return@withLock

                val nextSync = hero.id to timeZoneId
                if (nextSync == lastSyncedHeroTimeZone) return@withLock

                when (val updateResult = updateHeroTimeZone(hero.id, timeZoneId)) {
                    is DomainResult.Success -> {
                        lastSyncedHeroTimeZone = nextSync
                        Timber.d("Hero timezone synced: heroId=%d timezone=%s", hero.id, timeZoneId)
                    }

                    is DomainResult.Failure -> {
                        Timber.w(
                            "Hero timezone sync failed: heroId=%d timezone=%s error=%s",
                            hero.id,
                            timeZoneId,
                            updateResult.error,
                        )
                    }
                }
            }
        }
    }

    private fun currentTimeZoneId(): String = timeZoneProvider().ifBlank { "UTC" }
}
