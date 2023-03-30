package com.tangem.sdk.ui.widget

import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.TextView
import com.google.android.material.chip.Chip
import com.tangem.common.CompletionResult
import com.tangem.common.StringsLocator
import com.tangem.common.UserCodeType
import com.tangem.operations.resetcode.ResetPinService
import com.tangem.sdk.AndroidStringLocator
import com.tangem.sdk.R
import com.tangem.sdk.SessionViewDelegateState
import com.tangem.sdk.extensions.hide
import com.tangem.sdk.extensions.show
import com.tangem.sdk.ui.widget.leapfrogWidget.LeapfrogWidget

class ResetCodesWidget(mainView: View) : BaseSessionDelegateStateWidget(mainView) {
    private val tvTitle: TextView = mainView.findViewById(R.id.tv_title)
    private val leapfrogContainer: FrameLayout = mainView.findViewById(R.id.leapfrog_views_container)
    private val tvMessageTitle: TextView = mainView.findViewById(R.id.tvMessageTitle)
    private val tvMessageBody: TextView = mainView.findViewById(R.id.tvMessageSubtitle)
    private val btnContinue: Button = mainView.findViewById(R.id.btnContinue)
    private val chipCardCurrent: Chip = mainView.findViewById(R.id.chip_card_current)
    private val chipCardLinked: Chip = mainView.findViewById(R.id.chip_card_linked)

    private val leapfrogWidget = LeapfrogWidget(leapfrogContainer)

    override fun setState(params: SessionViewDelegateState) {
        val state = params as? SessionViewDelegateState.ResetCodes ?: return

        val stringLocator = AndroidStringLocator(mainView.context)

        val pinId = when (state.type) {
            UserCodeType.AccessCode -> StringsLocator.ID.PIN_1
            UserCodeType.Passcode -> StringsLocator.ID.PIN_2
        }

        tvTitle.text = stringLocator.getString(
            StringsLocator.ID.PIN_RESET_CODE_FORMAT,
            stringLocator.getString(pinId).lowercase()
        )

        tvMessageTitle.text = state.state.getMessageTitle(stringLocator)
        tvMessageBody.text = state.state.getMessageBody(stringLocator)

        btnContinue.setOnClickListener { state.callback(CompletionResult.Success(true)) }

        when (state.state) {
            ResetPinService.State.NeedScanResetCard -> {
                chipCardCurrent.hide()
                chipCardLinked.hide()

                leapfrogWidget.initViews()
                leapfrogWidget.unfold()
                leapfrogWidget.leap {
                    chipCardCurrent.show()
                    chipCardLinked.hide()
                }
            }
            ResetPinService.State.NeedScanConfirmationCard -> {
                chipCardCurrent.hide()
                chipCardLinked.hide()
                leapfrogWidget.leap {
                    chipCardLinked.show()
                }
            }
            ResetPinService.State.NeedWriteResetCard -> {
                chipCardCurrent.hide()
                chipCardLinked.hide()
                leapfrogWidget.leap {
                    chipCardCurrent.show()
                }
            }
        }
    }
}
