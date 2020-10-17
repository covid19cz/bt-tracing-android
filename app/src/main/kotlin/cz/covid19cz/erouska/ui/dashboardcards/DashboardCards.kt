package cz.covid19cz.erouska.ui.dashboardcards

import android.os.Bundle
import android.view.View
import cz.covid19cz.erouska.AppConfig
import cz.covid19cz.erouska.R
import cz.covid19cz.erouska.databinding.FragmentDashboardCardsBinding
import cz.covid19cz.erouska.ui.base.BaseFragment
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.fragment_dashboard_cards.*
import kotlinx.android.synthetic.main.view_data_item.view.*

@AndroidEntryPoint
class DashboardCards :
    BaseFragment<FragmentDashboardCardsBinding, DashboardCardsVM>(R.layout.fragment_dashboard_cards, DashboardCardsVM::class) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        enableUpInToolbar(true, IconType.UP)

        // TODO Change RC string (in defaults and on server side) to march Figma
        dash_card_no_risky_encounter.card_title = AppConfig.noEncounterHeader
        dash_card_no_risky_encounter.card_subtitle = resources.getString(R.string.dashboard_body_no_contact, viewModel.lastUpdateDate.value, viewModel.lastUpdateTime.value) + "\n" +AppConfig.encounterUpdateFrequency

        // Samples
        dash_bluetooth_off.card_on_button_click = View.OnClickListener { requestEnableBt() }
        dash_location_off.card_on_button_click = View.OnClickListener { requestLocationEnable() }
        dash_bluetooth_location_off.card_on_button_click = View.OnClickListener { requestLocationEnable() }
    }
}
