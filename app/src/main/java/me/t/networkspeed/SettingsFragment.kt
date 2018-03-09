package me.t.networkspeed

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.preference.ListPreference
import android.preference.Preference
import android.preference.PreferenceFragment
import android.preference.SwitchPreference
import android.view.View

/**
 * Created by lai
 * on 2018/1/25.
 */

class SettingsFragment : PreferenceFragment() {

    private val sPreferenceEnable = "preference_network_speed_enable"
    private val sPreferenceDelay = "preference_network_speed_check_delay"

    private lateinit var mDelayPreference: ListPreference
    private lateinit var mEnablePreference: SwitchPreference

    private lateinit var mSharePreference: SharedPreferences
    private lateinit var mContext: Context

    private var isServiceStart: Boolean = false
    private var mDelay = "2000"

    override fun onAttach(context: Context?) {
        super.onAttach(context)
        mContext = context!!
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        addPreferencesFromResource(R.xml.preference)
        mSharePreference = preferenceManager.sharedPreferences
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setup()
    }

    private fun setup() {
        mEnablePreference = findPreference(sPreferenceEnable) as SwitchPreference
        mDelayPreference = findPreference(sPreferenceDelay) as ListPreference

        val enable = mSharePreference.getBoolean(sPreferenceEnable, false)
        mDelay = mSharePreference.getString(sPreferenceDelay, "500")

        if (enable) startService(mDelay.toLong())

        mDelayPreference.summary = getDelayMessage(mDelay)

        mEnablePreference.onPreferenceChangeListener = Preference.OnPreferenceChangeListener { _, newValue ->
            val isEnable = newValue as Boolean
            when (isEnable) {
                true -> startService(mDelay.toLong())
                false -> stopService()
            }
            true
        }

        mDelayPreference.onPreferenceChangeListener = Preference.OnPreferenceChangeListener { preference, newValue ->
            stopService()
            mDelay = newValue as String
            preference.summary = getDelayMessage(mDelay)
            startService(mDelay.toLong())
            true
        }
    }

    private fun startService(delay: Long) {
        if (isServiceStart) stopService()
        val intent = Intent(mContext, NetworkSpeedService::class.java)
        intent.putExtra("delay", delay)
        mContext.startService(intent)
        isServiceStart = true
    }

    private fun stopService() {
        if (isServiceStart) {
            mContext.stopService(Intent(mContext, NetworkSpeedService::class.java))
            isServiceStart = false
        }
    }

    override fun onDestroyView() {
        stopService()
        super.onDestroyView()
    }

    private fun getDelayMessage(value: String): String {
        return when (value) {
            "500" -> "500ms"
            "1000" -> "1s"
            "2000" -> "2s"
            else -> ""
        }
    }
}
