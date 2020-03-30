package cz.covid19cz.app.ui.dashboard

import android.content.*
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.core.content.ContextCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.tbruyelle.rxpermissions2.RxPermissions
import cz.covid19cz.app.AppConfig
import cz.covid19cz.app.R
import cz.covid19cz.app.databinding.FragmentPermissionssDisabledBinding
import cz.covid19cz.app.ext.hasLocationPermission
import cz.covid19cz.app.ext.isBatterySaverEnabled
import cz.covid19cz.app.ext.isLocationEnabled
import cz.covid19cz.app.service.CovidService
import cz.covid19cz.app.ui.base.BaseFragment
import cz.covid19cz.app.ui.dashboard.event.DashboardCommandEvent
import cz.covid19cz.app.utils.Auth
import cz.covid19cz.app.utils.L
import cz.covid19cz.app.utils.logoutWhenNotSignedIn
import io.reactivex.disposables.CompositeDisposable
import org.koin.android.ext.android.inject


class DashboardFragment : BaseFragment<FragmentPermissionssDisabledBinding, DashboardVM>(
    R.layout.fragment_dashboard,
    DashboardVM::class
) {

    private val compositeDisposable = CompositeDisposable()
    private lateinit var rxPermissions: RxPermissions
    private val localBroadcastManager by inject<LocalBroadcastManager>()

    private val serviceStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            intent?.let {
                when (it.action) {
                    CovidService.ACTION_MASK_STARTED -> viewModel.serviceRunning.value = true
                    CovidService.ACTION_MASK_STOPPED -> viewModel.serviceRunning.value = false
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        registerServiceStateReceivers()

        rxPermissions = RxPermissions(this)

        subscribe(DashboardCommandEvent::class) { commandEvent ->
            when (commandEvent.command) {
                DashboardCommandEvent.Command.TURN_ON -> tryStartBtService()
                DashboardCommandEvent.Command.TURN_OFF -> context?.let {
                    it.startService(CovidService.stopService(it))
                }
                DashboardCommandEvent.Command.PAUSE -> pauseService()
                DashboardCommandEvent.Command.RESUME -> resumeService()
                DashboardCommandEvent.Command.UPDATE_STATE -> {
                    checkRequirements(onFailed = {
                        navigate(R.id.action_nav_dashboard_to_nav_bt_disabled)
                    }, onBatterySaverEnabled = {
                        showBatterySaverDialog()
                    })
                }
            }
        }

        checkIfServiceIsRunning()

        checkIfSignedIn()

        viewModel.init()
    }

    private fun showBatterySaverDialog() {
        MaterialAlertDialogBuilder(context)
            .setMessage(R.string.battery_saver_disabled_desc)
            .setPositiveButton(R.string.disable_battery_saver)
            { dialog, which ->
                dialog.dismiss()
                navigateToBatterySaverSettings()
            }
            .setNegativeButton(getString(R.string.confirmation_button_close))
            { dialog, which -> dialog.dismiss() }
            .show()
    }

    private fun navigateToBatterySaverSettings() {
        val batterySaverIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
            Intent(Settings.ACTION_BATTERY_SAVER_SETTINGS)
        } else {
            val intent = Intent()
            intent.component = ComponentName(
                "com.android.settings",
                "com.android.settings.Settings\$BatterySaverSettingsActivity"
            )
            intent
        }
        startActivity(batterySaverIntent)
    }

    private fun checkIfSignedIn() {
        if (!Auth.isSignedIn()) {
            logoutWhenNotSignedIn()
        }
    }

    private fun checkIfServiceIsRunning() {
        if (CovidService.isRunning(requireContext())) {
            L.d("Service Covid is running")
            viewModel.serviceRunning.value = true
        } else {
            viewModel.serviceRunning.value = false
            L.d("Service Covid is not running")
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        enableUpInToolbar(false)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.dashboard, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_share -> {
                shareApp()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onBluetoothEnabled() {
        tryStartBtService()
    }

    override fun onDestroy() {
        compositeDisposable.dispose()
        localBroadcastManager.unregisterReceiver(serviceStateReceiver)
        super.onDestroy()
    }

    private fun resumeService() {
        requireContext().run {
            startService(CovidService.resume(this))
        }
    }

    private fun pauseService() {
        requireContext().run {
            startService(CovidService.pause(this))
        }
    }

    private fun registerServiceStateReceivers() {
        localBroadcastManager.registerReceiver(
            serviceStateReceiver,
            IntentFilter(CovidService.ACTION_MASK_STARTED)
        )
        localBroadcastManager.registerReceiver(
            serviceStateReceiver,
            IntentFilter(CovidService.ACTION_MASK_STOPPED)
        )
    }

    private fun checkRequirements(onPassed: () -> Unit = {}, onFailed: () -> Unit = {}, onBatterySaverEnabled: () -> Unit = {}) {
        with(requireContext()) {
            if (viewModel.bluetoothRepository.hasBle(this)) {
                if (!viewModel.bluetoothRepository.isBtEnabled() || !isLocationEnabled() || !hasLocationPermission()) {
                    onFailed()
                    return
                } else if (isBatterySaverEnabled()) {
                    onBatterySaverEnabled()
                } else {
                    onPassed()
                }
            } else {
                showSnackBar(R.string.error_ble_unsupported)
            }
        }
    }

    private fun tryStartBtService() {
        checkRequirements(
            {
                ContextCompat.startForegroundService(
                    requireContext(),
                    CovidService.startService(requireContext())
                )
            },
            { navigate(R.id.action_nav_dashboard_to_nav_bt_disabled) },
            { showBatterySaverDialog() }
        )
    }

    private fun shareApp() {
        val text = getString(R.string.share_app_text, AppConfig.shareAppDynamicLink)
        shareLink(text)
    }

    private fun shareLink(link: String) {
        val intent = Intent(Intent.ACTION_SEND)
        intent.type = "text/plain"
        intent.putExtra(Intent.EXTRA_TEXT, link)
        startActivity(Intent.createChooser(intent, getString(R.string.share_app_title)))
    }
}
