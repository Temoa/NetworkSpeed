package me.t.networkspeed

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.preference.PreferenceManager

/**
 * Created by lai
 * on 2018/3/9.
 */
class BootStartReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent?.action == Intent.ACTION_BOOT_COMPLETED) {
            val sp = PreferenceManager.getDefaultSharedPreferences(context)
            val isBootStart = sp.getBoolean("preference_network_speed_boot_start", false)
            if (isBootStart) {
                val startIntent = Intent(context, NetworkSpeedService::class.java)
                context?.startService(startIntent)
            }
        }
    }
}