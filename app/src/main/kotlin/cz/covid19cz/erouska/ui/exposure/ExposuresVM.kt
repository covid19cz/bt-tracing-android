package cz.covid19cz.erouska.ui.exposure

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import arch.viewmodel.BaseArchViewModel
import cz.covid19cz.erouska.exposurenotifications.ExposureNotificationsRepository
import cz.covid19cz.erouska.ext.daysSinceEpochToDateString
import cz.covid19cz.erouska.ui.exposure.event.ExposuresCommandEvent
import cz.covid19cz.erouska.utils.L
import kotlinx.coroutines.launch

class ExposuresVM(private val exposureNotificationsRepo: ExposureNotificationsRepository) :
    BaseArchViewModel() {

    val lastExposureDate = MutableLiveData<String>()

    fun checkExposures(demo: Boolean) {
        // TODO Check if there were any exposures in last 14 days
        // If yes -> Show RECENT_EXPOSURE
        // If not and there are some exposures in the past -> Show NO_RECENT_EXPOSURES
        // If there are NO exposures in the DB -> Show NO_EXPOSURES

        if (!demo) {
            viewModelScope.launch {
                kotlin.runCatching {
                    exposureNotificationsRepo.getLastRiskyExposure()
                }.onSuccess {
                    publish(
                        ExposuresCommandEvent(
                            if (it != null) {
                                lastExposureDate.value =
                                    it.daysSinceEpoch.daysSinceEpochToDateString()
                                ExposuresCommandEvent.Command.RECENT_EXPOSURE
                            } else {
                                ExposuresCommandEvent.Command.NO_RECENT_EXPOSURES
                            }
                        )
                    )
                }.onFailure {
                    L.e(it)
                }
            }
        } else {
            lastExposureDate.value =
                ((System.currentTimeMillis() / 1000 / 60 / 60 / 24) - 3).toInt()
                    .daysSinceEpochToDateString()
            publish(
                ExposuresCommandEvent(ExposuresCommandEvent.Command.RECENT_EXPOSURE)
            )
        }
    }

    fun debugRecentExp() {
        publish(ExposuresCommandEvent(ExposuresCommandEvent.Command.RECENT_EXPOSURE))
    }

    fun debugExp() {
        publish(ExposuresCommandEvent(ExposuresCommandEvent.Command.NO_RECENT_EXPOSURES))
    }

}