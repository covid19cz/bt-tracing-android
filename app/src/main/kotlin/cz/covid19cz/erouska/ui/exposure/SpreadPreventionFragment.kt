package cz.covid19cz.erouska.ui.exposure

import android.os.Bundle
import android.view.View
import cz.covid19cz.erouska.AppConfig
import cz.covid19cz.erouska.R
import cz.covid19cz.erouska.databinding.FragmentSpreadPreventionBinding
import cz.covid19cz.erouska.ui.base.BaseFragment
import kotlinx.android.synthetic.main.fragment_spread_prevention.*

class SpreadPreventionFragment : BaseFragment<FragmentSpreadPreventionBinding, SpreadPreventionVM>(
    R.layout.fragment_spread_prevention,
    SpreadPreventionVM::class
) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initText()
    }

    private fun initText() {
        activity?.title = AppConfig.spreadPreventionUITitle

        prevention_text.text = AppConfig.preventionBody
    }


}