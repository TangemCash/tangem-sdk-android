package com.tangem.tangem_demo.ui.settings

import android.content.Context
import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import androidx.preference.EditTextPreference
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceScreen
import androidx.preference.SwitchPreferenceCompat
import com.tangem.tangem_demo.DemoApplication

/**
 * Created by Anton Zhilenkov on 24/03/2021.
 */
class SettingsFragment : PreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        val context = preferenceManager.context
        preferenceScreen = preferenceManager.createPreferenceScreen(context)
        ThemeGroup.create(requireActivity(), preferenceScreen)
        InitialMessageGroup.create(preferenceScreen)
    }

    companion object {
        val nightMode = "nightMode"
        val initialMessageEnabled = "initialMessageEnabled"
        val initialMessageHeader = "initialMessageHeader"
        val initialMessageBody = "initialMessageBody"
    }
}

class ThemeGroup {
    companion object {
        fun create(activity: FragmentActivity, screen: PreferenceScreen) {
            PreferenceCategory(screen.context).apply {
                screen.addPreference(this)
                title = "Day night mode"
                addPreference(createThemeSwitcher(activity, screen))
            }
        }

        private fun createThemeSwitcher(activity: FragmentActivity, screen: PreferenceScreen): Preference {
            val themeSwitch = SwitchPreferenceCompat(screen.context)
            themeSwitch.key = SettingsFragment.nightMode
            themeSwitch.title = "Enable night mode"
            themeSwitch.onPreferenceChangeListener = Preference.OnPreferenceChangeListener { preference, newValue ->
                val value = newValue as? Boolean ?: return@OnPreferenceChangeListener false

                val appContext = activity.applicationContext as DemoApplication
                appContext.switchToNighMode(value)
                return@OnPreferenceChangeListener true
            }
            return themeSwitch
        }
    }
}

class InitialMessageGroup {
    companion object {
        fun create(screen: PreferenceScreen) {
            PreferenceCategory(screen.context).apply {
                screen.addPreference(this)
                title = "Initial message"
                addPreference(createInitialMessagePreference(screen))
                addPreference(initialMessageText(screen, SettingsFragment.initialMessageHeader, "Header"))
                addPreference(initialMessageText(screen, SettingsFragment.initialMessageBody, "Body"))
            }
        }

        private fun createInitialMessagePreference(screen: PreferenceScreen): Preference {
            return SwitchPreferenceCompat(screen.context).apply {
                key = SettingsFragment.initialMessageEnabled
                title = "Attach to the commands"
            }
        }

        private fun initialMessageText(screen: PreferenceScreen, key: String, title: String): Preference {
            return EditSummaryPreference(screen.context).apply {
                this.key = key
                this.title = title
            }
        }
    }

}

class EditSummaryPreference(context: Context) : EditTextPreference(context) {
    override fun getSummary(): CharSequence = text ?: ""
}
