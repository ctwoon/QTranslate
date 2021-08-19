package eu.ctwoon.qtranslate

import android.app.*
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.os.Environment
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import kotlinx.coroutines.*
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

object OTA : CoroutineScope by MainScope() {

    var needDownload = false

    lateinit var changelog: String
    lateinit var parseddString: String

    lateinit var version: String

    lateinit var handler: CoroutineExceptionHandler

    lateinit var broadcastReceiver: BroadcastReceiver

    private fun checkBS(callback: (Boolean) -> Unit) {
        launch(handler) {
            try {
                val request: Request =
                    Request.Builder().url("https://api.github.com/repos/ctwoon/qtranslate/releases/latest").build()
                withContext(Dispatchers.IO) {
                    val response = OkHttpClient().newCall(request).execute()
                    parseddString = response.body!!.string()
                    val parsedString = JSONObject(parseddString)
                    if (parsedString.getString("name") != BuildConfig.VERSION_NAME) {
                        version = parsedString.getString("name")
                        changelog = parsedString.getString("body")
                        needDownload = true
                    } else needDownload = false
                }
                callback.invoke(needDownload)
            } catch (e: java.lang.Exception) {
                throw e
            }
        }
    }

    @JvmStatic
    fun download(context: Context, b: Boolean) {
        broadcastReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent) {
                when (intent.extras!!.getString("action_name")) {
                    "action_download" -> {
                        downloadApk(context!!)
                    }
                }
            }
        }

        try {
            context.unregisterReceiver(broadcastReceiver)
        } catch (e: Exception) {
        }

        handler = CoroutineExceptionHandler { _, _ -> }

        checkBS { needDownload ->
            if (needDownload && b) {
                showAlert(context)
            } else if (needDownload && !b) {
                context.registerReceiver(broadcastReceiver, IntentFilter("OTA_NOTIF"))
                if (Build.VERSION.SDK_INT >= 26) {
                    val channel = NotificationChannel("channel01", "name",
                        NotificationManager.IMPORTANCE_HIGH) // for heads up notifications

                    channel.description = "description"

                    val notificationManager: NotificationManager? = context.getSystemService(NotificationManager::class.java)

                    notificationManager!!.createNotificationChannel(channel)
                }

                val intentDownload = Intent(context, NotificationActionService::class.java)
                    .setAction("action_download")
                val pendingIntentDownload = PendingIntent.getBroadcast(
                    context, 0,
                    intentDownload, PendingIntent.FLAG_UPDATE_CURRENT
                )

                val notification: Notification = NotificationCompat.Builder(context, "channel01")
                    .setSmallIcon(R.drawable.ic_baseline_translate_24)
                    .setContentTitle("QTranslate • $version")
                    .setContentText(changelog)
                    .setDefaults(Notification.DEFAULT_ALL)
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .addAction(R.drawable.ic_baseline_arrow_downward_24, context.getString(R.string.download), pendingIntentDownload)
                    .build()

                val notificationManager = NotificationManagerCompat.from(context)

                notificationManager.notify(1337, notification)
            }
        }
    }

    fun showAlert(context: Context) {
        val notificationManager = NotificationManagerCompat.from(context)
        try {
            notificationManager.cancel(1337)
        } catch (e: Exception) {
        }
        val builder = AlertDialog.Builder(context)
        builder.setTitle("QTranslate • $version")
            .setMessage(changelog)
            .setPositiveButton(context.getString(R.string.download)) { _, _ ->
                downloadApk(context)
            }
        builder.show()
    }

    fun downloadApk(context: Context) {
        val notificationManager = NotificationManagerCompat.from(context)
        notificationManager.cancel(1337)
        val request = DownloadManager.Request(Uri.parse("https://github.com/ctwoon/qtranslate/releases/latest/download/app.apk"))

        request.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_MOBILE or DownloadManager.Request.NETWORK_WIFI)
        request.setTitle("QTranslate v$version")
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
        request.setDestinationInExternalFilesDir(context, Environment.DIRECTORY_DOWNLOADS, "ota.apk")

        val manager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        manager.enqueue(request)
    }
}