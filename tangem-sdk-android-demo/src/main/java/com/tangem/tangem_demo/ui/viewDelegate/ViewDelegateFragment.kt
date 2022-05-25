package com.tangem.tangem_demo.ui.viewDelegate

import android.os.Bundle
import android.view.View
import com.tangem.Log
import com.tangem.SessionViewDelegate
import com.tangem.common.CompletionResult
import com.tangem.common.card.Card
import com.tangem.tangem_demo.DemoActivity
import com.tangem.tangem_demo.R
import com.tangem.tangem_demo.ui.BaseFragment
import kotlinx.android.synthetic.main.fg_view_delegate.*
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import java.io.PrintWriter
import java.io.StringWriter
import kotlin.coroutines.CoroutineContext

/**
 * Created by Anton Zhilenkov on 23/12/2020.
 */
class ViewDelegateFragment : BaseFragment() {

    private lateinit var delegate: SessionViewDelegate

    private val parentJob = Job()
    private val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
        val sw = StringWriter()
        throwable.printStackTrace(PrintWriter(sw))
        val exceptionAsString: String = sw.toString()
        Log.error { exceptionAsString }
        throw throwable
    }
    private val coroutineContext: CoroutineContext = parentJob + Dispatchers.IO + exceptionHandler
    private val scope = CoroutineScope(coroutineContext)

    override fun getLayoutId(): Int = R.layout.fg_view_delegate

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        delegate = (requireActivity() as DemoActivity).viewDelegate
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initActionsList()
    }

    private fun initActionsList() {
        val actionsList = mutableListOf<ViewDelegateAction>(
            TagConnectTagLostError(),
            SecurityDelay(),
            SecurityDelayPinFails(),
            WrongCard(),
            OnError(),
            RequestAccessCode(),
            RequestPasscode(),
            RequestUserCode(),
            RequestPinSetup(),
            SingleRequestAccessCode(false),
            SingleRequestAccessCode(true),
        )

        actionsList.forEach {
            it.inflateViews(actions_container)
            it.init(delegate, scope)
        }
    }

    override fun handleCommandResult(result: CompletionResult<*>) {
    }

    override fun onCardChanged(card: Card?) {
    }
}
