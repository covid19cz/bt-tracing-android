package cz.covid19cz.erouska.exposurenotifications

import android.content.Context
import android.util.Base64
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.google.android.gms.nearby.exposurenotification.*
import cz.covid19cz.erouska.AppConfig
import cz.covid19cz.erouska.db.SharedPrefsRepository
import cz.covid19cz.erouska.exposurenotifications.worker.SelfCheckerWorker
import cz.covid19cz.erouska.net.ExposureServerRepository
import cz.covid19cz.erouska.net.FirebaseFunctionsRepository
import cz.covid19cz.erouska.net.model.*
import cz.covid19cz.erouska.ui.senddata.ReportExposureException
import cz.covid19cz.erouska.ui.senddata.VerifyException
import cz.covid19cz.erouska.utils.L
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

@Singleton
class ExposureNotificationsRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val client: ExposureNotificationClient,
    private val server: ExposureServerRepository,
    private val cryptoTools: ExposureCryptoTools,
    private val prefs: SharedPrefsRepository,
    private val firebaseFunctionsRepository: FirebaseFunctionsRepository
) {

    suspend fun start() = suspendCoroutine<Void> { cont ->
        client.start()
            .addOnSuccessListener {
                cont.resume(it)
            }.addOnFailureListener {
                cont.resumeWithException(it)
            }
    }

    suspend fun stop() = suspendCoroutine<Void> { cont ->
        client.stop()
            .addOnSuccessListener {
                cont.resume(it)
            }.addOnFailureListener {
                cont.resumeWithException(it)
            }
    }

    suspend fun isEnabled(): Boolean = suspendCoroutine { cont ->
        client.isEnabled
            .addOnSuccessListener {
                cont.resume(it)
            }.addOnFailureListener {
                cont.resumeWithException(it)
            }
    }

    suspend fun provideDiagnosisKeys(
        keys: DownloadedKeys
    ): Boolean = suspendCoroutine { cont ->

        setDiagnosisKeysMapping()

        if (keys.isValid()) {
            if (keys.files.isNotEmpty()) {
                L.i("Importing keys")
                client.provideDiagnosisKeys(keys.files)
                    .addOnSuccessListener {
                        L.i("Import success")
                        prefs.setLastKeyImport()

                        prefs.setLastKeyExportFileName(keys.getLastUrl())
                        cont.resume(true)
                    }.addOnFailureListener {
                        cont.resumeWithException(it)
                    }
            } else {
                L.i("Import skipped (no new data)")
                prefs.setLastKeyImport()
                cont.resume(true)
            }
        } else {
            L.i("Import skipped (invalid data)")
            cont.resume(true)
        }
    }

    private fun setDiagnosisKeysMapping() {
        if (System.currentTimeMillis() - prefs.getLastSetDiagnosisKeysDataMapping() > AppConfig.diagnosisKeysDataMappingLimitDays * 24 * 60 * 60 * 1000) {
            val daysList = AppConfig.daysSinceOnsetToInfectiousness
            val daysToInfectiousness = mutableMapOf<Int, Int>()
            for (i in -14..14) {
                daysToInfectiousness[i] = daysList[i + 14]
            }
            val mapping = DiagnosisKeysDataMapping.DiagnosisKeysDataMappingBuilder()
                .setDaysSinceOnsetToInfectiousness(daysToInfectiousness)
                .setInfectiousnessWhenDaysSinceOnsetMissing(Infectiousness.NONE)
                .setReportTypeWhenMissing(AppConfig.reportTypeWhenMissing)
                .build()
            try {
                client.setDiagnosisKeysDataMapping(mapping)
            } catch (t: Throwable) {
                L.e(t)
            } finally {
                prefs.setLastSetDiagnosisKeysDataMapping()
            }
        }
    }

    suspend fun getDailySummaries(): List<DailySummary> = suspendCoroutine { cont ->

        val reportTypeWeights = prefs.getReportTypeWeights() ?: AppConfig.reportTypeWeights
        val attenuationBucketThresholdDb =
            prefs.getAttenuationBucketThresholdDb() ?: AppConfig.attenuationBucketThresholdDb
        val attenuationBucketWeights =
            prefs.getAttenuationBucketWeights() ?: AppConfig.attenuationBucketWeights
        val infectiousnessWeights =
            prefs.getInfectiousnessWeights() ?: AppConfig.infectiousnessWeights

        client.getDailySummaries(
            DailySummariesConfig.DailySummariesConfigBuilder().apply {
                for (i in 0..5) {
                    setReportTypeWeight(i, reportTypeWeights[i])
                }
                setAttenuationBuckets(attenuationBucketThresholdDb, attenuationBucketWeights)
                for (i in 0..2) {
                    setInfectiousnessWeight(i, infectiousnessWeights[i])
                }
                setMinimumWindowScore(AppConfig.minimumWindowScore)
            }.build()
        ).addOnSuccessListener {
            cont.resume(it)
        }.addOnFailureListener {
            cont.resumeWithException(it)
        }
    }

    suspend fun getLastRiskyExposure(): DailySummary? {
        return getDailySummaries().maxBy { it.daysSinceEpoch }
    }

    suspend fun getTemporaryExposureKeyHistory(): List<TemporaryExposureKey> =
        suspendCoroutine { cont ->
            client.temporaryExposureKeyHistory.addOnSuccessListener {
                cont.resume(it)
            }.addOnFailureListener {
                cont.resumeWithException(it)
            }
        }

    suspend fun getExposureWindows(): List<ExposureWindow> = suspendCoroutine { cont ->
        client.exposureWindows
            .addOnSuccessListener {
                cont.resume(it)
            }.addOnFailureListener {
                cont.resumeWithException(it)
            }
    }

    suspend fun reportExposureWithoutVerification(): Int {
        val keys = getTemporaryExposureKeyHistory()
        val request = ExposureRequest(keys.map {
            TemporaryExposureKeyDto(
                Base64.encodeToString(
                    it.keyData,
                    Base64.NO_WRAP
                ), it.rollingStartIntervalNumber, it.rollingPeriod
            )
        }, null, null, null, prefs.getRevisionToken())
        val response = server.reportExposure(request)
        if (response.errorMessage != null) {
            throw ReportExposureException(response.errorMessage, response.code)
        }
        prefs.saveRevisionToken(response.revisionToken)
        return response.insertedExposures ?: 0
    }

    suspend fun reportExposureWithVerification(code: String): Int {
        val keys = getTemporaryExposureKeyHistory()
        val verifyResponse = server.verifyCode(VerifyCodeRequest(code))
        if (verifyResponse.token != null) {
            L.i("Verify code success")
            val hmackey = cryptoTools.newHmacKey()
            val keyHash = cryptoTools.hashedKeys(keys, hmackey)
            val token = verifyResponse.token

            val certificateResponse = server.verifyCertificate(
                VerifyCertificateRequest(token, keyHash)
            )
            if (certificateResponse.error != null) {
                throw VerifyException(certificateResponse.error, certificateResponse.errorCode)
            }
            L.i("Verify certificate success")

            val request = ExposureRequest(
                keys.map {
                    TemporaryExposureKeyDto(
                        Base64.encodeToString(
                            it.keyData,
                            Base64.NO_WRAP
                        ), it.rollingStartIntervalNumber, it.rollingPeriod
                    )
                },
                certificateResponse.certificate,
                hmackey,
                null,
                prefs.getRevisionToken(),
                healthAuthorityID = "cz.covid19cz.erouska"
            )
            val response = server.reportExposure(request)
            response.errorMessage?.let {
                L.e("Report exposure failed: $it")
                throw ReportExposureException(it, response.code)
            }
            L.i("Report exposure success, ${response.insertedExposures} keys inserted")
            prefs.saveRevisionToken(response.revisionToken)
            return response.insertedExposures ?: 0
        } else {
            throw VerifyException(verifyResponse.error, verifyResponse.errorCode)
        }
    }

    fun checkExposure(context: Context) {
        GlobalScope.launch {
            kotlin.runCatching {
                val lastExposure = getLastRiskyExposure()?.daysSinceEpoch
                val lastNotifiedExposure = prefs.getLastNotifiedExposure()
                if (lastExposure != null && lastExposure != lastNotifiedExposure) {
                    firebaseFunctionsRepository.registerNotification()
                    LocalNotificationsHelper.showRiskyExposureNotification(context)
                    prefs.setLastNotifiedExposure(lastExposure)
                }
            }
        }
    }

    fun scheduleSelfChecker() {
        val constraints = Constraints.Builder().build()
        val worker = PeriodicWorkRequestBuilder<SelfCheckerWorker>(
            AppConfig.selfCheckerPeriodHours,
            TimeUnit.HOURS
        ).setConstraints(constraints)
            .addTag(SelfCheckerWorker.TAG)
            .build()

        WorkManager.getInstance(context)
            .enqueueUniquePeriodicWork(
                SelfCheckerWorker.TAG,
                ExistingPeriodicWorkPolicy.REPLACE,
                worker
            )
    }

    suspend fun isEligibleToDownloadKeys(): Boolean {
        return isEnabled() && System.currentTimeMillis() - prefs.getLastKeyImport() >= AppConfig.keyImportPeriodHours * 60 * 60 * 1000
    }
}