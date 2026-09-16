package com.workeasy.clockincompanion.data.offline

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.workeasy.clockincompanion.domain.publisher.ClockEventPublisher
import com.workeasy.clockincompanion.domain.store.ClockEventStore
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class EventFlushWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val store: ClockEventStore,
    private val publisher: ClockEventPublisher,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val pending = store.getPending()
        for (event in pending) {
            val published = publisher.publish(event)
            if (published) {
                store.markSynced(event.id)
            } else {
                return Result.retry()
            }
        }
        return Result.success()
    }

    companion object {
        const val WORK_NAME = "event_flush_worker"

        fun schedule(context: Context) {
            val request = OneTimeWorkRequestBuilder<EventFlushWorker>()
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build(),
                )
                .build()
            WorkManager.getInstance(context)
                .enqueueUniqueWork(WORK_NAME, ExistingWorkPolicy.REPLACE, request)
        }
    }
}
