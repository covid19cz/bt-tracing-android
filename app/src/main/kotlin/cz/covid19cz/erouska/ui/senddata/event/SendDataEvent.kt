package cz.covid19cz.erouska.ui.senddata.event

import arch.event.LiveEvent

class SendDataCommandEvent(val command: Command, val errorMessage: String? = null) : LiveEvent() {

    enum class Command {
        INIT,
        PROCESSING,
        CODE_VALID,
        CODE_INVALID,
        CODE_EXPIRED,
        CODE_EXPIRED_OR_USED,
        DATA_SEND_FAILURE,
        DATA_SEND_SUCCESS
    }

}

sealed class SendDataState
object SendDataInitState : SendDataState()
object SendDataFailedState : SendDataState()
object SendDataSuccessState : SendDataState()