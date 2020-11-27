package cz.covid19cz.erouska.ui.update.legacy

import android.os.Bundle
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.text.HtmlCompat
import androidx.lifecycle.observe
import cz.covid19cz.erouska.AppConfig
import cz.covid19cz.erouska.R
import cz.covid19cz.erouska.databinding.FragmentLegacyUpdateBinding
import cz.covid19cz.erouska.ext.hide
import cz.covid19cz.erouska.ext.show
import cz.covid19cz.erouska.ext.showWeb
import cz.covid19cz.erouska.ui.base.BaseFragment
import cz.covid19cz.erouska.ui.update.legacy.event.LegacyUpdateEvent
import cz.covid19cz.erouska.utils.CustomTabHelper
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.fragment_legacy_update.*
import javax.inject.Inject

@AndroidEntryPoint
class LegacyUpdateFragment : BaseFragment<FragmentLegacyUpdateBinding, LegacyUpdateVM>(
    R.layout.fragment_legacy_update,
    LegacyUpdateVM::class
) {

    @Inject
    internal lateinit var customTabHelper: CustomTabHelper

    private var isEFGS: Boolean = false
    private var isFullscreen: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        isEFGS = arguments?.let {
            LegacyUpdateFragmentArgs.fromBundle(it).efgs
        } ?: false

        isFullscreen = arguments?.let {
            LegacyUpdateFragmentArgs.fromBundle(it).fullscreen
        } ?: false

        (activity as? AppCompatActivity)?.supportActionBar?.setDisplayShowTitleEnabled(!isFullscreen)

        viewModel.state.value = if (isEFGS) LegacyUpdateEvent.LegacyUpdateEFGS else LegacyUpdateEvent.LegacyUpdateExpansion

        viewModel.state.observe(this) {
            when (it) {
                LegacyUpdateEvent.LegacyUpdateEFGS -> showEFGSNews()
                LegacyUpdateEvent.LegacyUpdateExpansion -> showExpansionNews()
                LegacyUpdateEvent.LegacyUpdatePhoneNumbers -> showPhoneNumberNews()
                LegacyUpdateEvent.LegacyUpdateActiveNotification -> showActiveNotificationNews()
                LegacyUpdateEvent.LegacyUpdatePrivacy -> showPrivacyNews()
                LegacyUpdateEvent.LegacyUpdateFinish -> finish()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        (activity as? AppCompatActivity)?.supportActionBar?.setDisplayShowTitleEnabled(true)
    }

    private fun showEFGSNews() {
        viewModel.sharedPrefsRepository.setEFGSIntroduced(true)
        enableUpInToolbar(false)

        legacy_update_checkbox.show()
        legacy_update_body.textAlignment = View.TEXT_ALIGNMENT_TEXT_START
        legacy_update_img.setImageResource(R.drawable.ic_update_expansion)
        legacy_update_header.text = getString(R.string.efgs_header)

        legacy_update_body.text = getString(R.string.efgs_boundaries) + "\n\n" + getString(R.string.efgs_visit, AppConfig.efgsDays) + "\n\n" + AppConfig.efgsSupportedCountries + "\n\n" +  getString(R.string.efgs_settings)

        legacy_update_checkbox.text = getString(R.string.efgs_check)
        legacy_update_button.text = getString(R.string.legacy_update_button_continue)
        legacy_update_button.setOnClickListener { finish() }

        legacy_update_checkbox.isChecked = viewModel.sharedPrefsRepository.isTraveller()
        legacy_update_checkbox.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                viewModel.sharedPrefsRepository.setTraveller(true)
            } else {
                viewModel.sharedPrefsRepository.setTraveller(false)
            }
        }
    }


    private fun showExpansionNews() {
        enableUpInToolbar(true, IconType.CLOSE)
        legacy_update_checkbox.hide()
        legacy_update_body.textAlignment = View.TEXT_ALIGNMENT_CENTER
        legacy_update_img.setImageResource(R.drawable.ic_update_expansion)
        legacy_update_header.text = getString(R.string.legacy_update_expansion_header)
        legacy_update_body.text = getString(R.string.legacy_update_expansion_body)
        legacy_update_button.text = getString(R.string.legacy_update_button_continue)
        legacy_update_button.setOnClickListener { next() }
    }

    private fun showActiveNotificationNews() {
        enableUpInToolbar(true, IconType.UP)
        legacy_update_checkbox.hide()
        legacy_update_body.textAlignment = View.TEXT_ALIGNMENT_CENTER
        legacy_update_img.setImageResource(R.drawable.ic_update_active_notification)
        legacy_update_header.text = getString(R.string.legacy_update_active_notification_header)
        legacy_update_body.text = getString(R.string.legacy_update_active_notification_body)
        legacy_update_button.text = getString(R.string.legacy_update_button_continue)
        legacy_update_button.setOnClickListener { next() }
    }

    private fun showPhoneNumberNews() {
        enableUpInToolbar(true, IconType.UP)
        legacy_update_checkbox.hide()
        legacy_update_body.textAlignment = View.TEXT_ALIGNMENT_CENTER
        legacy_update_img.setImageResource(R.drawable.ic_update_phone)
        legacy_update_header.text = getString(R.string.legacy_update_phone_header)
        legacy_update_body.text = getString(R.string.legacy_update_phone_body)
        legacy_update_button.text = getString(R.string.legacy_update_button_continue)
        legacy_update_button.setOnClickListener { next() }
    }

    private fun showPrivacyNews() {
        enableUpInToolbar(true, IconType.UP)
        legacy_update_checkbox.hide()
        legacy_update_body.textAlignment = View.TEXT_ALIGNMENT_CENTER
        legacy_update_img.setImageResource(R.drawable.ic_update_privacy)
        legacy_update_header.text = getString(R.string.legacy_update_privacy_header)
        legacy_update_body.text = HtmlCompat.fromHtml(
            getString(R.string.legacy_update_privacy_body, AppConfig.conditionsOfUseUrl),
            HtmlCompat.FROM_HTML_MODE_LEGACY
        )

        legacy_update_button.text = getString(R.string.legacy_update_button_close)
        legacy_update_body.setOnClickListener {
            showWeb(
                AppConfig.conditionsOfUseUrl,
                customTabHelper
            )
        }
        legacy_update_button.setOnClickListener { finish() }
    }

    private fun finish() {
        viewModel.finish()
        navController().navigateUp()
    }

    private fun next() {
        viewModel.next()
    }

    private fun previous() {
        viewModel.previous()
    }

    private fun isFirstScreen(): Boolean {
        return viewModel.state.value == if (isEFGS) LegacyUpdateEvent.LegacyUpdateEFGS else LegacyUpdateEvent.LegacyUpdateExpansion
    }

    override fun onBackPressed(): Boolean {
        return if (!isFirstScreen()) {
            previous()
            true
        } else {
            activity?.finish()
            false
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return onBackPressed()
    }

}
