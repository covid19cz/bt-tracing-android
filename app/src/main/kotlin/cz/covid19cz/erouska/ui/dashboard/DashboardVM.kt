package cz.covid19cz.erouska.ui.dashboard

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.OnLifecycleEvent
import androidx.lifecycle.viewModelScope
import arch.livedata.SafeMutableLiveData
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import cz.covid19cz.erouska.db.SharedPrefsRepository
import cz.covid19cz.erouska.exposurenotifications.ExposureNotificationsRepository
import cz.covid19cz.erouska.net.ExposureServerRepository
import cz.covid19cz.erouska.ui.base.BaseVM
import cz.covid19cz.erouska.ui.dashboard.event.BluetoothDisabledEvent
import cz.covid19cz.erouska.ui.dashboard.event.DashboardCommandEvent
import cz.covid19cz.erouska.ui.dashboard.event.GmsApiErrorEvent
import cz.covid19cz.erouska.utils.L
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class DashboardVM(
    private val exposureNotificationsRepository: ExposureNotificationsRepository,
    private val exposureNotificationsServerRepository: ExposureServerRepository,
    private val prefs: SharedPrefsRepository
) : BaseVM() {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    val serviceRunning = SafeMutableLiveData(prefs.isExposureNotificationsEnabled())
    val lastUpdate = MutableLiveData<String>()

    init {
        // TODO Check last download time
        // If lastDownload - now > 48 h -> publish DashboardCommandEvent.Command.DATA_OBSOLETE

        // TODO Check last exposure
        // If last exposure occured in less than 14 days -> publish DashboardCommandEvent.Command.RECENT_EXPOSURE

        // TODO Check if EN API is off
        // If yes -> publish DashboardCommandEvent(DashboardCommandEvent.Command.EN_API_OFF)
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
    fun onResume() {
        if (auth.currentUser == null) {
            publish(DashboardCommandEvent(DashboardCommandEvent.Command.NOT_ACTIVATED))
            return
        }

        checkForObsoleteData()

        val formatter = SimpleDateFormat("d.M.yyyy H:mm", Locale.getDefault())
        val lastImportTimestamp = prefs.getLastKeyImport()
        if (lastImportTimestamp != 0L) {
            lastUpdate.value = formatter.format(Date(prefs.getLastKeyImport()))
        }

        viewModelScope.launch {
            kotlin.runCatching {
                val result = exposureNotificationsRepository.isEnabled()
                if (result && !exposureNotificationsServerRepository.isKeyDownloadScheduled()) {
                    exposureNotificationsRepository.start()
                    exposureNotificationsServerRepository.scheduleKeyDownload()
                }
                return@runCatching result
            }.onSuccess { enabled ->
                L.d("Exposure Notifications enabled $enabled")
                onExposureNotificationsStateChanged(enabled)
                if (enabled) {
                    checkForRiskyExposure()
                }
            }.onFailure {
                if (it is ApiException) {
                    publish(GmsApiErrorEvent(it.status))
                }
                L.e(it)
            }
        }
    }

    fun stop() {
        viewModelScope.launch {
            kotlin.runCatching {
                exposureNotificationsRepository.stop()
            }.onSuccess {
                onExposureNotificationsStateChanged(false)
                exposureNotificationsServerRepository.unscheduleKeyDownload()
                L.d("Exposure Notifications stopped")
                publish(DashboardCommandEvent(DashboardCommandEvent.Command.TURN_OFF))
            }.onFailure {
                L.e(it)
            }
        }
    }

    fun start() {
        if (exposureNotificationsRepository.isBluetoothEnabled()) {
            viewModelScope.launch {
                kotlin.runCatching {
                    exposureNotificationsRepository.start()
                }.onSuccess {
                    onExposureNotificationsStateChanged(true)
                    exposureNotificationsServerRepository.scheduleKeyDownload()
                    L.d("Exposure Notifications started")
                }.onFailure {
                    onExposureNotificationsStateChanged(false)
                    if (it is ApiException) {
                        publish(GmsApiErrorEvent(it.status))
                    }
                    L.e(it)
                }
            }
        } else {
            publish(BluetoothDisabledEvent())
        }
    }

    private fun onExposureNotificationsStateChanged(enabled : Boolean){
        serviceRunning.value = enabled
        prefs.setExposureNotificationsEnabled(enabled)
    }

    fun checkForRiskyExposure() {
        viewModelScope.launch {
            runCatching {
                exposureNotificationsRepository.getLastRiskyExposure()
            }.onSuccess {
                it?.let {
                    showExposure()
                }
            }.onFailure {
                L.e(it)
            }
        }
    }

    fun checkForObsoleteData(){
        if (prefs.hasOutdatedKeyData()){
            publish(DashboardCommandEvent(DashboardCommandEvent.Command.DATA_OBSOLETE))
        }
    }

    private fun showExposure() {
        publish(DashboardCommandEvent(DashboardCommandEvent.Command.RECENT_EXPOSURE))
    }

    fun unregister() {
        FirebaseAuth.getInstance().signOut()
    }
}
