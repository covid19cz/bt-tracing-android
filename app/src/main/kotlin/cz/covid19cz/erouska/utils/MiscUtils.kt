package cz.covid19cz.erouska.utils

import java.text.DecimalFormat
import java.text.NumberFormat

object SignNumberFormat {
    @JvmStatic
    fun format(value: Number): String {
        return NumberFormat.getInstance().also { it.showSign() }.format(value)
    }
}

fun NumberFormat.showSign() {
    if (this is DecimalFormat) {
        this.positivePrefix = "+"
    }
}

fun isAndroid11andUp() = android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R
