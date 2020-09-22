package cz.covid19cz.erouska

import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings
import cz.covid19cz.erouska.utils.L

object AppConfig {
    const val FIREBASE_REGION = "europe-west1"

    private val firebaseRemoteConfig = FirebaseRemoteConfig.getInstance()

    // Exposure Notifications Settings
    val reportTypeWeights
        get() = firebaseRemoteConfig.getString("v2_reportTypeWeights").split(";").map { it.toDouble() }
    val infectiousnessWeights
        get() = firebaseRemoteConfig.getString("v2_infectiousnessWeights").split(";")
            .map { it.toDouble() }
    val attenuationBucketThresholdDb
        get() = firebaseRemoteConfig.getString("v2_attenuationBucketThresholdDb").split(";")
            .map { it.toInt() }
    val attenuationBucketWeights
        get() = firebaseRemoteConfig.getString("v2_attenuationBucketWeights").split(";")
            .map { it.toDouble() }
    val minimumWindowScore
        get() = firebaseRemoteConfig.getDouble("v2_minimumWindowScore")
    val daysSinceOnsetToInfectiousness
        get() = firebaseRemoteConfig.getString("v2_daysSinceOnsetToInfectiousness").split(";")
            .map { it.toInt() }
    val diagnosisKeysDataMappingLimitDays
        get() = firebaseRemoteConfig.getLong("v2_diagnosisKeysDataMappingLimitDays").toInt()
    val supportEmail
        get() = firebaseRemoteConfig.getDouble("v2_supportEmail")
    val reportTypeWhenMissing
        get() = firebaseRemoteConfig.getLong("v2_reportTypeWhenMissing").toInt()
    val shareAppDynamicLink
        get() = firebaseRemoteConfig.getString("v2_shareAppDynamicLink")
    val chatBotLink
        get() = firebaseRemoteConfig.getString("v2_chatBotLink")
    val helpMarkdown
        get() = firebaseRemoteConfig.getString("v2_helpMarkdown")
    val minSupportedVersionCodeAndroid
        get() = firebaseRemoteConfig.getLong("v2_minSupportedVersionCodeAndroid")
    val riskyEncountersTitle
        get() = firebaseRemoteConfig.getString("v2_riskyEncountersTitleAn")
    val noEncounterHeader
        get() = firebaseRemoteConfig.getString("v2_noEncounterHeader")
    val noEncounterBody
        get() = firebaseRemoteConfig.getString("v2_noEncounterBody")
    val exposureUITitle
        get() = firebaseRemoteConfig.getString("v2_exposureUITitle")
    val symptomsUITitle
        get() = firebaseRemoteConfig.getString("v2_symptomsUITitle")
    val spreadPreventionUITitle
        get() = firebaseRemoteConfig.getString("v2_spreadPreventionUITitle")
    val recentExposuresUITitle
        get() = firebaseRemoteConfig.getString("v2_recentExposuresUITitle")
    val symptomsContentJson
        get() = firebaseRemoteConfig.getString("v2_symptomsContentJson")
    val preventionContentJson
        get() = firebaseRemoteConfig.getString("v2_preventionContentJson")
    val encounterWarning
        get() = firebaseRemoteConfig.getString("v2_encounterWarning")
    val selfCheckerPeriodHours
        get() = firebaseRemoteConfig.getLong("v2_selfCheckerPeriodHours")
    val keyExportUrl
        get() = firebaseRemoteConfig.getString("v2_keyExportUrl")
    val keyImportPeriodHours
        get() = firebaseRemoteConfig.getLong("v2_keyImportPeriodHours")
    val keyImportDataOutdatedHours
        get() = firebaseRemoteConfig.getLong("v2_keyImportDataOutdatedHours")
    val contactsContentJson
        get() = firebaseRemoteConfig.getString("v2_contactsContentJson")
    val riskyEncountersWithSymptoms
        get() = firebaseRemoteConfig.getString("v2_riskyEncountersWithSymptoms")
    val riskyEncountersWithoutSymptoms
        get() = firebaseRemoteConfig.getString("v2_riskyEncountersWithoutSymptoms")
    val currentMeasuresUrl
        get() = firebaseRemoteConfig.getString("v2_currentMeasuresUrl")
    val minGmsVersionCode
        get() = firebaseRemoteConfig.getLong("v2_minGmsVersionCode")
    val conditionsOfUseUrl
        get() = firebaseRemoteConfig.getString("v2_conditionsOfUseUrl")
    val verificationServerApiKey
        get() = firebaseRemoteConfig.getString("v2_verificationServerApiKey")
    val showChatBotLink
        get() = firebaseRemoteConfig.getBoolean("v2_showChatBotLink")

    init {
        val configSettings: FirebaseRemoteConfigSettings = FirebaseRemoteConfigSettings.Builder()
            .setMinimumFetchIntervalInSeconds(if (BuildConfig.DEBUG) 0 else 3600)
            .build()
        firebaseRemoteConfig.setConfigSettingsAsync(configSettings)

        firebaseRemoteConfig.setDefaultsAsync(R.xml.remote_config_defaults).addOnCompleteListener {
            print()
        }
    }

    fun fetchRemoteConfig() {
        firebaseRemoteConfig.fetchAndActivate().addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val updated = task.result
                L.d("Config params updated: $updated")
                print()
            } else {
                L.e("Config params update failed")
                task.exception?.printStackTrace()
            }
        }
    }

    private fun print() {
        for (item in firebaseRemoteConfig.all) {
            L.d("${item.key}: ${item.value.asString()}")
        }
    }
}