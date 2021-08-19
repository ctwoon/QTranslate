package eu.ctwoon.qtranslate

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent


class NotificationActionService : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action.toString() == "action_download")
            context.sendBroadcast(
                Intent("OTA_NOTIF")
                    .putExtra("action_name", intent.action)
            )
    }
}