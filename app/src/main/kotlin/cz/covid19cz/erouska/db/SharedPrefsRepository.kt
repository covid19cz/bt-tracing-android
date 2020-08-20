package cz.covid19cz.erouska.db

import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.SharedPreferences

class SharedPrefsRepository(c: Context) {

    companion object {
        const val APP_PAUSED = "preference.app_paused"
        const val LAST_KEY_EXPORT = "preference.last_export"
        const val LAST_KEY_EXPORT_TIME = "preference.last_export_time"
        const val EHRID = "preference.ehrid"
    }

    private val prefs: SharedPreferences = c.getSharedPreferences("prefs", MODE_PRIVATE)

    fun lastKeyExportFileName(): String {
        return prefs.getString(LAST_KEY_EXPORT, "") ?: ""
    }

    fun setLastKeyExportFileName(filename: String) {
        prefs.edit().putString(LAST_KEY_EXPORT, filename).apply()
    }

    fun addLastKeyExportTime(time: String) {
        val timestamps = prefs.getString(LAST_KEY_EXPORT_TIME, "") ?: ""
        val timestampsList = timestamps.split(",").toMutableList()
        timestampsList.add(time)
        prefs.edit().putString(LAST_KEY_EXPORT_TIME, timestampsList.joinToString(",").trim(',')).apply()
    }

    fun keyExportTimeHistory(): List<String> {
        val timestamps = prefs.getString(LAST_KEY_EXPORT_TIME, "") ?: ""
        return timestamps.split(",")
    }

    fun lastKeyExportTime(): String {
        val history = prefs.getString(LAST_KEY_EXPORT_TIME, "") ?: ""
        val historyList = history.split(",")
        return historyList.max() ?: ""
    }

    fun clearLastKeyExportFileName() {
        prefs.edit().remove(LAST_KEY_EXPORT).apply()
    }

    fun clearLastKeyExportTime() {
        prefs.edit().remove(LAST_KEY_EXPORT_TIME).apply()
    }

    fun isUpdateFromLegacyVersion() = prefs.contains(APP_PAUSED)

    fun markUpdateFromLegacyVersionCompleted() {
        prefs.edit().remove(APP_PAUSED).apply()
    }

    fun saveEhrid(ehrid: String) {
        prefs.edit().putString(EHRID, ehrid).apply()
    }

    fun isActivated(): Boolean {
        return prefs.contains(EHRID)
    }

    fun getEhrid(): String {
        return checkNotNull(prefs.getString(EHRID, null))
    }
}