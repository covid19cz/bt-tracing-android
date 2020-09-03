package cz.covid19cz.erouska.net

import android.content.Context
import androidx.work.*
import com.google.gson.GsonBuilder
import cz.covid19cz.erouska.AppConfig
import cz.covid19cz.erouska.BuildConfig
import cz.covid19cz.erouska.R
import cz.covid19cz.erouska.db.SharedPrefsRepository
import cz.covid19cz.erouska.exposurenotifications.worker.DownloadKeysWorker
import cz.covid19cz.erouska.net.api.KeyServerApi
import cz.covid19cz.erouska.net.api.VerificationServerApi
import cz.covid19cz.erouska.net.model.*
import cz.covid19cz.erouska.utils.L
import kotlinx.coroutines.*
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.DataInputStream
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.net.URL
import java.util.concurrent.TimeUnit


class ExposureServerRepository(
    private val context: Context,
    private val prefs: SharedPrefsRepository
) {

    companion object {
        private val KEY_EXPORT_INDEX = "${AppConfig.keyExportUrl}/index.txt"
    }

    private val okhttpBuilder by lazy {
        val builder = OkHttpClient.Builder()
        if (BuildConfig.DEBUG) {
            builder.addInterceptor(HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            })
        }
        builder
    }

    private val keyServerClient by lazy {
        Retrofit.Builder()
            .baseUrl(context.getString(R.string.key_server_base_url))
            .addConverterFactory(GsonConverterFactory.create(GsonBuilder().create()))
            .client(okhttpBuilder.build())
            .build().create(KeyServerApi::class.java)
    }

    private val verificationServerClient by lazy {
        Retrofit.Builder()
            .baseUrl(context.getString(R.string.verification_server_base_url))
            .addConverterFactory(GsonConverterFactory.create(GsonBuilder().create()))
            .client(okhttpBuilder.addInterceptor {
                val request = it.request().newBuilder()
                    .addHeader("X-API-Key", context.getString(R.string.verification_server_api_key))
                    .build()
                it.proceed(request)
            }.build())
            .build().create(VerificationServerApi::class.java)
    }

    suspend fun reportExposure(request: ExposureRequest): ExposureResponse {
        return withContext(Dispatchers.IO) {
            keyServerClient.reportExposure(request)
        }
    }

    suspend fun verifyCode(request: VerifyCodeRequest): VerifyCodeResponse {
        return withContext(Dispatchers.IO) {
            verificationServerClient.verifyCode(request)
        }
    }

    suspend fun verifyCertificate(request: VerifyCertificateRequest): VerifyCertificateResponse {
        return withContext(Dispatchers.IO) {
            verificationServerClient.verifyCertificate(request)
        }
    }

    suspend fun cover(request: CoverRequest): CoverResponse {
        return withContext(Dispatchers.IO) {
            verificationServerClient.cover(request)
        }
    }

    suspend fun downloadKeyExport(): List<File> {
        return withContext(Dispatchers.IO) {
            val lastDownloadedFile = prefs.lastKeyExportFileName()

            val urlContent = readURLContent(KEY_EXPORT_INDEX)

            var fileNames = urlContent.split('\n')

            // Find index of last downloaded file and get everything after it
            val indexOfLastDownload = fileNames.indexOf(lastDownloadedFile)
            if (indexOfLastDownload != -1) {
                fileNames = fileNames.subList(indexOfLastDownload + 1, fileNames.size)
            }

            val extractedFiles = mutableListOf<File>()
            val indexUrls = fileNames.map { AppConfig.keyExportUrl + it }

            val downloads = mutableListOf<Deferred<File?>>()
            indexUrls.forEach {
                downloads.add(async { downloadFile(it) })
            }
            extractedFiles.addAll(downloads.awaitAll().filterNotNull())

            // If there weren't any new ZIPs for download, keep last download the same
            val newLastDownload =
                if (fileNames.isNotEmpty()) fileNames.last() else lastDownloadedFile

            prefs.setLastKeyExportFileName(newLastDownload)
            prefs.setLastKeyImport(System.currentTimeMillis())

            extractedFiles
        }
    }

    private fun readURLContent(url: String): String {
        val indexConnection = URL(url).openConnection()
        val indexInputStream = indexConnection.getInputStream()

        return indexInputStream.readBytes().toString(Charsets.UTF_8)
    }

    fun downloadFile(zipfile: String): File? {
        try {
            val dir = File(context.cacheDir.path + "/export/")
            val file =
                File(context.cacheDir.path + "/export/" + zipfile.substring(zipfile.lastIndexOf("/") + 1))
            dir.mkdirs()
            file.createNewFile()
            val u = URL(zipfile)
            val inputStream: InputStream = u.openStream()
            val dis = DataInputStream(inputStream)
            val buffer = ByteArray(1024)
            var length: Int
            val fos = FileOutputStream(file)
            while (dis.read(buffer).also { length = it } > 0) {
                fos.write(buffer, 0, length)
            }
            return file
        } catch (t: Throwable) {
            L.e(t)
        }
        return null
    }

    fun scheduleKeyDownload() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        val worker = PeriodicWorkRequestBuilder<DownloadKeysWorker>(
            AppConfig.keyImportPeriodHours,
            TimeUnit.HOURS
        ).setConstraints(constraints)
            .addTag(DownloadKeysWorker.TAG)
            .build()

        WorkManager.getInstance(context)
            .enqueueUniquePeriodicWork(
                DownloadKeysWorker.TAG,
                ExistingPeriodicWorkPolicy.REPLACE,
                worker
            )
    }

    fun unscheduleKeyDownload(){
        WorkManager.getInstance(context)
            .cancelUniqueWork(
                DownloadKeysWorker.TAG
            )
    }

    suspend fun isKeyDownloadScheduled() : Boolean{
        return WorkManager.getInstance(context).getWorkInfosForUniqueWork(DownloadKeysWorker.TAG).await().size != 0
    }

    fun deleteFiles() {
        val extractedDir = File(context.cacheDir.path + "/export/")
        extractedDir.deleteRecursively()
        prefs.clearLastKeyExportFileName()
        prefs.clearLastKeyImportTime()
    }
}