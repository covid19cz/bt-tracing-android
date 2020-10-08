package cz.covid19cz.erouska.ui.sandbox

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import cz.covid19cz.erouska.R
import cz.covid19cz.erouska.databinding.FragmentSandboxBinding
import cz.covid19cz.erouska.exposurenotifications.ExposureNotificationsErrorHandling
import cz.covid19cz.erouska.ui.base.BaseFragment
import cz.covid19cz.erouska.ui.dashboard.event.GmsApiErrorEvent
import cz.covid19cz.erouska.ui.sandbox.event.SnackbarEvent
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SandboxFragment :
    BaseFragment<FragmentSandboxBinding, SandboxVM>(R.layout.fragment_sandbox, SandboxVM::class) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        subscribe(GmsApiErrorEvent::class) {
            ExposureNotificationsErrorHandling.handle(it, this)
        }

        subscribe(SnackbarEvent::class) {
            showSnackBar(it.text)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == ExposureNotificationsErrorHandling.REQUEST_GMS_ERROR_RESOLUTION && resultCode == Activity.RESULT_OK) {
            viewModel.refreshTeks()
        }
    }
}
