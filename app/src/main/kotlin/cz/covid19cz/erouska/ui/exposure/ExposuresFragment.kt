package cz.covid19cz.erouska.ui.exposure

import android.os.Bundle
import android.view.View
import cz.covid19cz.erouska.AppConfig
import cz.covid19cz.erouska.BuildConfig
import cz.covid19cz.erouska.R
import cz.covid19cz.erouska.databinding.FragmentExposuresBinding
import cz.covid19cz.erouska.ext.hide
import cz.covid19cz.erouska.ext.show
import cz.covid19cz.erouska.ui.base.BaseFragment
import cz.covid19cz.erouska.ui.exposure.event.ExposuresCommandEvent
import kotlinx.android.synthetic.main.fragment_exposures.*

class ExposuresFragment : BaseFragment<FragmentExposuresBinding, ExposuresVM>(
    R.layout.fragment_exposures,
    ExposuresVM::class
) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        enableUpInToolbar(true)

        if (BuildConfig.FLAVOR == "dev") {
            debug_buttons_container.show()
        }

        activity?.title = AppConfig.exposureUITitle

        symptoms_text.text = AppConfig.mainSymptoms
        spread_text.text = AppConfig.spreadPrevention
        earlier_exposures_text.text = AppConfig.earlierExposures
        earlier_exposures_button.text = AppConfig.earlierExposures
        symptoms_content.text = AppConfig.riskyEncountersWithSymptoms
        no_symptoms_content.text = AppConfig.riskyEncountersWithoutSymptoms

        viewModel.checkExposures()

        subscribe(ExposuresCommandEvent::class) {
            when (it.command) {
                ExposuresCommandEvent.Command.NO_EXPOSURES -> onNoExposures()
                ExposuresCommandEvent.Command.NO_RECENT_EXPOSURES -> onNoRecentExposures()
                ExposuresCommandEvent.Command.RECENT_EXPOSURE -> onRecentExposures()
            }
        }

        setupListeners()
    }

    private fun setupListeners() {
        symptoms_container.setOnClickListener { navigate(R.id.action_nav_dashboard_to_nav_main_symptoms) }
        spread_prevention_container.setOnClickListener { navigate(R.id.action_nav_dashboard_to_nav_spread_prevention) }
        earlier_exposures_container.setOnClickListener { navigate(R.id.action_nav_dashboard_to_nav_recent_exposures) }
        earlier_exposures_button.setOnClickListener { navigate(R.id.action_nav_dashboard_to_nav_recent_exposures) }
    }

    private fun onNoExposures() {
        no_exposures_header.text = AppConfig.noExposureHeader
        no_exposures_body.text = AppConfig.noExposureBody

        no_exposures_img.show()
        no_exposures_header.show()
        no_exposures_body.show()

        last_exposure.hide()

        divider_1.hide()
        divider_2.hide()
        symptoms_header.hide()
        symptoms_content.hide()
        no_symptoms_header.hide()
        no_symptoms_content.hide()

        symptoms_container.hide()
        spread_prevention_container.hide()
        earlier_exposures_container.hide()
        earlier_exposures_button.hide()
    }

    private fun onNoRecentExposures() {
        no_exposures_header.text = AppConfig.noExposureHeader
        no_exposures_body.text = AppConfig.noExposureBody

        no_exposures_img.show()
        no_exposures_header.show()
        no_exposures_body.show()
        earlier_exposures_button.show()

        last_exposure.hide()

        divider_1.hide()
        divider_2.hide()
        symptoms_header.hide()
        symptoms_content.hide()
        no_symptoms_header.hide()
        no_symptoms_content.hide()

        symptoms_container.hide()
        spread_prevention_container.hide()
        earlier_exposures_container.hide()
        bottom_separator.hide()
    }

    private fun onRecentExposures() {
        // TODO Mock date, replace with date of last exposure
        val mockDate = "14. 5. 2020"
        last_exposure.text = String.format(AppConfig.exposureBodyTop, mockDate)

        last_exposure.show()

        divider_1.show()
        divider_2.show()
        symptoms_header.show()
        symptoms_content.show()
        no_symptoms_header.show()
        no_symptoms_content.show()

        symptoms_container.show()
        spread_prevention_container.show()
        earlier_exposures_container.show()

        no_exposures_img.hide()
        no_exposures_header.hide()
        no_exposures_body.hide()
        earlier_exposures_button.hide()
    }

}
