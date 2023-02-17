package com.tangem.tangem_demo

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayoutMediator
import com.tangem.Log
import com.tangem.LogFormat
import com.tangem.SessionViewDelegate
import com.tangem.TangemSdk
import com.tangem.common.card.FirmwareVersion
import com.tangem.common.core.Config
import com.tangem.common.services.secure.SecureStorage
import com.tangem.tangem_demo.ui.separtedCommands.CommandListFragment
import com.tangem.tangem_demo.ui.settings.SettingsFragment
import com.tangem.tangem_demo.ui.viewDelegate.ViewDelegateFragment
import com.tangem.tangem_sdk_new.DefaultSessionViewDelegate
import com.tangem.tangem_sdk_new.extensions.createLogger
import com.tangem.tangem_sdk_new.extensions.initBiometricManager
import com.tangem.tangem_sdk_new.extensions.initNfcManager
import com.tangem.tangem_sdk_new.storage.create
import kotlinx.android.synthetic.main.activity_demo.*

class DemoActivity : AppCompatActivity() {

    val logger = TangemSdk.createLogger(LogFormat.StairsFormatter())

    lateinit var sdk: TangemSdk
        private set
    lateinit var viewDelegate: SessionViewDelegate
        private set

    private val pageChangeListeners = mutableListOf<(Int) -> Unit>()
    private val fragmentPages = listOf(
        CommandListFragment::class.java,
        ViewDelegateFragment::class.java,
        SettingsFragment::class.java
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        sdk = initSdk()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_demo)

        Log.addLogger(logger)
        viewPager.adapter = ViewPagerAdapter(fragmentPages, this)
        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                pageChangeListeners.forEach { it.invoke(position) }
            }
        })
        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            tab.text = fragmentPages[position].simpleName.replace("Fragment", "")
        }.attach()
    }

    fun listenPageChanges(callback: (Int) -> Unit) {
        pageChangeListeners.add(callback)
    }

    fun enableSwipe(enable: Boolean) {
        viewPager.isUserInputEnabled = enable
    }

    private fun initSdk(): TangemSdk {
        val config = Config().apply {
            linkedTerminal = false
            allowUntrustedCards = true
            filter.allowedCardTypes = FirmwareVersion.FirmwareType.values().toList()
        }
        val secureStorage = SecureStorage.create(this)
        val nfcManager = TangemSdk.initNfcManager(this)
        val authManager = TangemSdk.initBiometricManager(this, secureStorage)

        val viewDelegate = DefaultSessionViewDelegate(nfcManager, nfcManager.reader, this)
        viewDelegate.sdkConfig = config
        this.viewDelegate = viewDelegate

        return TangemSdk(
            reader = nfcManager.reader,
            viewDelegate = viewDelegate,
            secureStorage = secureStorage,
            config = config,
            biometricManager = authManager,
        )
    }

    class ViewPagerAdapter(
        private val fgList: List<Class<out Fragment>>,
        fgActivity: FragmentActivity,
    ) : FragmentStateAdapter(fgActivity) {

        override fun getItemCount(): Int = fgList.size

        override fun createFragment(position: Int): Fragment = fgList[position].newInstance()
    }
}
