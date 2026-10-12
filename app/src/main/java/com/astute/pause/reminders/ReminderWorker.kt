package com.astute.pause.reminders

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class ReminderWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val actions: ReminderActions,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val itemId = inputData.getLong(KEY_ITEM_ID, -1L)
        if (itemId <= 0) return Result.failure()
        when (inputData.getString(KEY_KIND)) {
            KIND_DECIDE -> actions.onDecideDue(itemId)
            KIND_NUDGE -> actions.onNudgeDue(itemId)
            KIND_FOLLOWUP -> actions.onFollowUpDue(itemId)
            else -> return Result.failure()
        }
        return Result.success()
    }

    companion object {
        const val KEY_ITEM_ID = "item_id"
        const val KEY_KIND = "kind"
        const val KIND_DECIDE = "decide"
        const val KIND_NUDGE = "nudge"
        const val KIND_FOLLOWUP = "followup"
    }
}
