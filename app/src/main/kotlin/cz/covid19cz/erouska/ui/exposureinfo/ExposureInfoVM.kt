package cz.covid19cz.erouska.ui.exposureinfo

import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.OnLifecycleEvent
import androidx.lifecycle.viewModelScope
import cz.covid19cz.erouska.db.SharedPrefsRepository
import cz.covid19cz.erouska.exposurenotifications.ExposureNotificationsRepository
import cz.covid19cz.erouska.ui.base.BaseVM
import kotlinx.coroutines.launch

class ExposureInfoVM @ViewModelInject constructor(private val exposureNotificationsRepo : ExposureNotificationsRepository, private val prefs : SharedPrefsRepository) : BaseVM(){

    val date = MutableLiveData<String>()
    var demo = false

    @OnLifecycleEvent(Lifecycle.Event.ON_CREATE)
    fun onCreate(){
        viewModelScope.launch {
            exposureNotificationsRepo.getLastRiskyExposure(demo)?.let {
                date.value = exposureNotificationsRepo.getLastRiskyExposure(demo)?.getDateString()
                prefs.setLastShownExposureInfo(it.daysSinceEpoch)
            }
        }
    }

    fun dismiss(){
        navigate(ExposureInfoFragmentDirections.actionNavExposureInfoToNavExposure())
    }
}