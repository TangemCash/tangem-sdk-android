package com.tangem.tangem_sdk_new.ui.widget

import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.TextView
import com.google.android.material.chip.Chip
import com.tangem.common.CompletionResult
import com.tangem.common.StringsLocator
import com.tangem.common.UserCodeType
import com.tangem.operations.resetcode.ResetPinService
import com.tangem.tangem_sdk_new.AndroidStringLocator
import com.tangem.tangem_sdk_new.R
import com.tangem.tangem_sdk_new.SessionViewDelegateState
import com.tangem.tangem_sdk_new.extensions.hide
import com.tangem.tangem_sdk_new.extensions.show
import com.tangem.tangem_sdk_new.ui.widget.leapfrogWidget.LeapfrogWidget

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

        tvTitle.text = AndroidStringLocator(mainView.context).getString(
            StringsLocator.ID.pin_reset_code_format,
            AndroidStringLocator(mainView.context).getString(
                when (state.type) {
                    UserCodeType.AccessCode -> StringsLocator.ID.pin1
                    UserCodeType.Passcode -> StringsLocator.ID.pin2
                }
            ).lowercase()
        )

        tvMessageTitle.text = state.state.getMessageTitle(AndroidStringLocator(mainView.context))
        tvMessageBody.text = state.state.getMessageBody(AndroidStringLocator(mainView.context))

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