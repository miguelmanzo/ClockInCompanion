package com.workeasy.clockincompanion.data.offline

import android.content.Context
import com.workeasy.clockincompanion.domain.store.OfflineFlushScheduler
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WorkManagerFlushScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
) : OfflineFlushScheduler {
    override fun schedule() {
        EventFlushWorker.schedule(context)
    }
}
