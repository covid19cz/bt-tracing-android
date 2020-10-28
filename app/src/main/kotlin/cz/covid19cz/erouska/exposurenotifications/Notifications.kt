package cz.covid19cz.erouska.exposurenotifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.annotation.StringRes
import androidx.core.app.NotificationCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.messaging.FirebaseMessaging
import cz.covid19cz.erouska.AppConfig
import cz.covid19cz.erouska.R
import cz.covid19cz.erouska.db.SharedPrefsRepository
import cz.covid19cz.erouska.ext.isNetworkAvailable
import cz.covid19cz.erouska.net.FirebaseFunctionsRepository
import cz.covid19cz.erouska.ui.main.MainActivity
import cz.covid19cz.erouska.utils.L
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class Notifications @Inject constructor(
    @ApplicationContext private val context: Context,
    private val prefs: SharedPrefsRepository,
    private val firebaseFunctionsRepository: FirebaseFunctionsRepository
) {

    companion object {
        const val CHANNEL_ID_EXPOSURE = "EXPOSURE"
        const val CHANNEL_ID_OUTDATED_DATA = "OUTDATED_DATA"
        const val CHANNEL_ID_NOT_RUNNING = "NOT_RUNNING"

        const val REQ_ID_EXPOSURE = 100
        const val REQ_ID_OUTDATED_DATA = 101
        const val REQ_ID_NOT_RUNNING = 102
    }

    fun showErouskaPausedNotification() {
        showNotification(
            R.string.dashboard_title_paused,
            R.string.notification_exposure_notifications_off_text,
            CHANNEL_ID_NOT_RUNNING
        )
    }

    fun showRiskyExposureNotification() {
        showNotification(
            R.string.notification_exposure_title,
            R.string.notification_exposure_text,
            CHANNEL_ID_EXPOSURE,
            autoCancel = true
        )
    }

    fun showOutdatedDataNotification() {
        showNotification(
            context.getString(R.string.notification_data_outdated_title),
            AppConfig.recentExposureNotificationTitle,
            CHANNEL_ID_OUTDATED_DATA
        )

    }

    private fun showNotification(
        @StringRes title: Int,
        @StringRes text: Int,
        channelId: String,
        autoCancel: Boolean = false
    ) {
        showNotification(
            context.getString(title),
            context.getString(text),
            channelId,
            autoCancel
        )
    }

    private fun showNotification(
        title: String,
        text: String,
        channelId: String,
        autoCancel: Boolean = false
    ) {
        val notificationIntent = Intent(context, MainActivity::class.java)
        notificationIntent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP

        val contentIntent = PendingIntent.getActivity(
            context,
            0,
            notificationIntent,
            PendingIntent.FLAG_UPDATE_CURRENT
        )

        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationCompat.Builder(context, channelId)
        } else {
            NotificationCompat.Builder(context)
        }
        builder.setContentTitle(title)
            .setContentText(text)
            .setSmallIcon(R.drawable.ic_notification_normal)
            .setContentIntent(contentIntent)
            .setAutoCancel(autoCancel)

        (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).notify(
            when (channelId) {
                CHANNEL_ID_EXPOSURE -> REQ_ID_EXPOSURE
                CHANNEL_ID_OUTDATED_DATA -> REQ_ID_OUTDATED_DATA
                CHANNEL_ID_NOT_RUNNING -> REQ_ID_NOT_RUNNING
                else -> 0
            }, builder.build()
        )
    }

    fun dismissNotRunningNotification() {
        dismissNotification(REQ_ID_NOT_RUNNING)
    }

    fun dismissOudatedDataNotification() {
        dismissNotification(REQ_ID_OUTDATED_DATA)
    }

    suspend fun getCurrentPushToken(): String {
        val pushToken = FirebaseMessaging.getInstance().token.await()
        L.d("Push token=$pushToken")
        return pushToken
    }

    private fun dismissNotification(id: Int) {
        (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).cancel(
            id
        )
    }

    fun init() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannel(
                CHANNEL_ID_EXPOSURE,
                context.getString(R.string.notification_channel_exposure),
                NotificationManager.IMPORTANCE_MAX,
                context
            )
            createNotificationChannel(
                CHANNEL_ID_NOT_RUNNING,
                context.getString(R.string.notification_channel_exposure_notifications_off),
                NotificationManager.IMPORTANCE_DEFAULT,
                context
            )
            createNotificationChannel(
                CHANNEL_ID_OUTDATED_DATA,
                context.getString(R.string.notification_channel_outdated_data),
                NotificationManager.IMPORTANCE_DEFAULT,
                context
            )
        }
        if (!prefs.isPushTokenRegistered() && context.isNetworkAvailable() && FirebaseAuth.getInstance().currentUser != null) {
            GlobalScope.launch {
                try {
                    firebaseFunctionsRepository.changePushToken(getCurrentPushToken())
                } catch (e: Throwable) {
                    L.e(e)
                }
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotificationChannel(
        id: String,
        name: String,
        importance: Int,
        context: Context
    ): NotificationChannel {
        val channel = NotificationChannel(id, name, importance)
        (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(
            channel
        )
        return channel
    }
}