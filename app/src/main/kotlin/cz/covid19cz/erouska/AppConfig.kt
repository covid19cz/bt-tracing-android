package cz.covid19cz.erouska

import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings
import cz.covid19cz.erouska.utils.L

object AppConfig {

    const val CSV_VERSION = 4
    const val FIREBASE_REGION = "europe-west3"

    private val firebaseRemoteConfig = FirebaseRemoteConfig.getInstance()
    val collectionSeconds
        get() = firebaseRemoteConfig.getLong("collectionSeconds")
    val waitingSeconds
        get() = firebaseRemoteConfig.getLong("waitingSeconds")
    val advertiseTxPower
        get() = overrideAdvertiseTxPower ?: firebaseRemoteConfig.getLong("advertiseTxPower").toInt()
    val advertiseMode
        get() = firebaseRemoteConfig.getLong("advertiseMode").toInt()
    val scanMode
        get() = firebaseRemoteConfig.getLong("scanMode").toInt()
    val smsTimeoutSeconds
        get() = firebaseRemoteConfig.getLong("smsTimeoutSeconds")
    val smsErrorTimeoutSeconds
        get() = firebaseRemoteConfig.getLong("smsErrorTimeoutSeconds")
    val advertiseRestartMinutes
        get() = firebaseRemoteConfig.getLong("advertiseRestartMinutes")
    val criticalExpositionRssi
        get() = firebaseRemoteConfig.getLong("criticalExpositionRssi").toInt()
    val criticalExpositionMinutes
        get() = firebaseRemoteConfig.getLong("criticalExpositionMinutes").toInt()
    val uploadWaitingMinutes
        get() = firebaseRemoteConfig.getLong("uploadWaitingMinutes").toInt()
    val persistDataDays
        get() = firebaseRemoteConfig.getLong("persistDataDays").toInt()
    val shareAppDynamicLink
        get() = firebaseRemoteConfig.getString("shareAppDynamicLink")
    val faqLink
        get() = firebaseRemoteConfig.getString("faqLink")
    val importantLink
        get() = firebaseRemoteConfig.getString("importantLink")
    val emergencyNumber
        get() = firebaseRemoteConfig.getString("emergencyNumber")
    val proclamationLink
        get() = firebaseRemoteConfig.getString("proclamationLink")
    val tutorialLink
        get() = firebaseRemoteConfig.getString("tutorialLink")
    val aboutApi
        get() = firebaseRemoteConfig.getString("aboutApi")
    val aboutLink
        get() = firebaseRemoteConfig.getString("aboutLink")
    val termsAndConditionsLink
        get() = firebaseRemoteConfig.getString("termsAndConditionsLink")
    val homepageLink
        get() = firebaseRemoteConfig.getString("homepageLink")
    val showBatteryOptimizationTutorial
        get() = firebaseRemoteConfig.getBoolean("showBatteryOptimizationTutorial")
    val batteryOptimizationAsusMarkdown
        get() = firebaseRemoteConfig.getString("batteryOptimizationAsusMarkdown")
    val batteryOptimizationLenovoMarkdown
        get() = firebaseRemoteConfig.getString("batteryOptimizationLenovoMarkdown")
    val batteryOptimizationSamsungMarkdown
        get() = firebaseRemoteConfig.getString("batteryOptimizationSamsungMarkdown")
    val batteryOptimizationSonyMarkdown
        get() = firebaseRemoteConfig.getString("batteryOptimizationSonyMarkdown")
    val batteryOptimizationXiaomiMarkdown
        get() = firebaseRemoteConfig.getString("batteryOptimizationXiaomiMarkdown")
    val batteryOptimizationHuaweiMarkdown
        get() = firebaseRemoteConfig.getString("batteryOptimizationHuaweiMarkdown")
    val helpMarkdown
        get() = firebaseRemoteConfig.getString("helpMarkdown")

    var overrideAdvertiseTxPower : Int? = null

    init {
        val configSettings: FirebaseRemoteConfigSettings = FirebaseRemoteConfigSettings.Builder()
            .setMinimumFetchIntervalInSeconds(if (BuildConfig.DEBUG) 0 else 3600)
            .build()
        firebaseRemoteConfig.setConfigSettingsAsync(configSettings)

        firebaseRemoteConfig.setDefaultsAsync(R.xml.remote_config_defaults).addOnCompleteListener {
            print()
        }
    }

    fun fetchRemoteConfig(){
        firebaseRemoteConfig.fetchAndActivate().addOnCompleteListener {
                task ->
            if (task.isSuccessful) {
                val updated = task.result
                L.d("Config params updated: $updated")
                print()
            } else {
                L.e("Config params update failed")
            }
        }
    }

    private fun print() {
        for (item in firebaseRemoteConfig.all) {
            L.d("${item.key}: ${item.value.asString()}")
        }
    }
}