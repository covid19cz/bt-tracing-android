package cz.covid19cz.erouska.exposurenotifications.worker

import android.content.Context
import androidx.hilt.Assisted
import androidx.hilt.work.WorkerInject
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import cz.covid19cz.erouska.db.SharedPrefsRepository
import cz.covid19cz.erouska.exposurenotifications.ExposureNotificationsRepository
import cz.covid19cz.erouska.exposurenotifications.LocalNotificationsHelper
import java.util.*

class SelfCheckerWorker @WorkerInject constructor(
    @Assisted val context: Context,
    @Assisted workerParams: WorkerParameters,
    private val prefs: SharedPrefsRepository,
    private val exposureNotificationsRepository: ExposureNotificationsRepository
) : CoroutineWorker(context, workerParams) {

    companion object {
        const val TAG = "SELF_CHECKER"
    }

    override suspend fun doWork(): Result {
        val hour = Calendar.getInstance(Locale.getDefault()).get(Calendar.HOUR_OF_DAY)
        if (hour in 9..19) {
            if (!exposureNotificationsRepository.isEnabled()) {
                LocalNotificationsHelper.showErouskaPausedNotification(context)
            }
            if (prefs.hasOutdatedKeyData()) {
                LocalNotificationsHelper.showOutdatedDataNotification(context)
            }
        }
        return Result.success()
    }

}