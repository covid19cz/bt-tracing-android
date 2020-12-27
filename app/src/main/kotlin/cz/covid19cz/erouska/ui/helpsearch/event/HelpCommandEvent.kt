package cz.covid19cz.erouska.ui.helpsearch.event

import arch.event.LiveEvent

class HelpCommandEvent(val command: Command) : LiveEvent() {

    enum class Command{
        GO_BACK,
        OPEN_CHATBOT
    }

}