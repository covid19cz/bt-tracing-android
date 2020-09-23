package cz.covid19cz.erouska.ui.permissions.bluetooth

import android.bluetooth.BluetoothAdapter
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.location.LocationManager
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import cz.covid19cz.erouska.R
import cz.covid19cz.erouska.ext.isBtEnabled
import cz.covid19cz.erouska.ext.isLocationEnabled
import cz.covid19cz.erouska.ext.shareApp
import cz.covid19cz.erouska.ui.permissions.BasePermissionsFragment
import cz.covid19cz.erouska.ui.permissions.bluetooth.event.PermissionsEvent
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class PermissionsDisabledFragment :
    BasePermissionsFragment<PermissionDisabledVM>(
        R.layout.fragment_permissionss_disabled,
        PermissionDisabledVM::class
    ) {

    private val btAndLocationReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent?) {
            val btEnabled = context.isBtEnabled()
            val locationEnabled = context.isLocationEnabled()

            when {
                !btEnabled && !locationEnabled -> viewModel.state.value = PermissionDisabledVM.ScreenState.LOCATION_BT_DISABLED
                !btEnabled -> viewModel.state.value = PermissionDisabledVM.ScreenState.BT_DISABLED
                !locationEnabled -> viewModel.state.value = PermissionDisabledVM.ScreenState.LOCATION_DISABLED
            }

            if (btEnabled && locationEnabled) {
                navigate(R.id.action_nav_bt_disabled_to_nav_dashboard)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableUpInToolbar(false)
        viewModel.initViewModel()

        subscribe(PermissionsEvent::class) {
            when (it.command) {
                PermissionsEvent.Command.ENABLE_BT -> requestEnableBt()
                PermissionsEvent.Command.ENABLE_LOCATION -> requestLocationEnable()
                PermissionsEvent.Command.ENABLE_BT_LOCATION -> requestLocationEnable()
            }
        }
    }

    override fun onStart() {
        super.onStart()
        context?.registerReceiver(btAndLocationReceiver, IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED))
        context?.registerReceiver(
            btAndLocationReceiver,
            IntentFilter(LocationManager.PROVIDERS_CHANGED_ACTION)
        )
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.dashboard, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_share -> {
                requireContext().shareApp()
                true
            }
            R.id.nav_about -> {
                navigate(R.id.nav_about)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onStop() {
        context?.unregisterReceiver(btAndLocationReceiver)
        super.onStop()
    }
}