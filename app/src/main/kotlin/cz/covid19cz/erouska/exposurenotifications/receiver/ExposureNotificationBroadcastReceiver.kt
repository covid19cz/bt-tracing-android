/*
 * Copyright 2020 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package cz.covid19cz.erouska.exposurenotifications.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.google.android.gms.nearby.exposurenotification.ExposureNotificationClient
import cz.covid19cz.erouska.db.SharedPrefsRepository
import cz.covid19cz.erouska.exposurenotifications.ExposureNotificationsRepository
import cz.covid19cz.erouska.exposurenotifications.LocalNotificationsHelper
import cz.covid19cz.erouska.net.ExposureServerRepository
import cz.covid19cz.erouska.net.FirebaseFunctionsRepository
import cz.covid19cz.erouska.utils.L
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.koin.core.KoinComponent
import org.koin.core.inject

/**
 * Broadcast receiver for callbacks from exposure notification API.
 */
class ExposureNotificationBroadcastReceiver : BroadcastReceiver(), KoinComponent {

    private val exposureNotificationsRepository : ExposureNotificationsRepository by inject()
    private val prefs: SharedPrefsRepository by inject()
    private val firebaseFunctionsRepository: FirebaseFunctionsRepository by inject()

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == ExposureNotificationClient.ACTION_EXPOSURE_STATE_UPDATED) {
            checkExposure(context)
        } else if (intent.action == ExposureNotificationClient.ACTION_EXPOSURE_NOT_FOUND){
            L.d("Exposure not found")
        }
    }

    private fun checkExposure(context: Context) {
        GlobalScope.launch {
            kotlin.runCatching {
                val lastExposure = exposureNotificationsRepository.getLastRiskyExposure()?.daysSinceEpoch
                val lastNotifiedExposure = prefs.getLastNotifiedExposure()
                if (lastExposure != null && lastNotifiedExposure != 0 && lastExposure != lastNotifiedExposure) {
                    firebaseFunctionsRepository.registerNotification()
                    LocalNotificationsHelper.showRiskyExposureNotification(context)
                    prefs.setLastNotifiedExposure(lastExposure)
                }
            }

        }

    }
}