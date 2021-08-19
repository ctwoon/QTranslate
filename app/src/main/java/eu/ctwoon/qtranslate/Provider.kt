package eu.ctwoon.qtranslate

import android.content.Context
import android.content.SharedPreferences
import androidx.preference.PreferenceManager
import kotlinx.coroutines.*
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import org.deepl.DeepLTranslater
import java.util.*

object Provider : CoroutineScope by MainScope() {

    @JvmStatic
    fun translateText(txt: String?, tl: String?, context: Context, callback: (String) -> Unit) {

        val prefs: SharedPreferences = PreferenceManager
            .getDefaultSharedPreferences(context)
        val a = prefs.getInt("trans", 2)

        when (a) {
            2 -> {
                launch {
                    try {
                        withContext(Dispatchers.IO) {
                            val a = DeepLTranslater().translate(txt, "auto",
                                tl?.uppercase(Locale.getDefault()), null)
                            withContext(Dispatchers.Main) {
                                callback.invoke(a)
                            }
                        }
                    } catch (e: Exception) {
                        callback.invoke("qtranslate error" + e.message.toString())
                    }
                }
            }
            1 -> {
                launch {
                    try {
                        withContext(Dispatchers.IO) {
                            val url =
                                URL("https://translate.yandex.net/api/v1/tr.json/translate?srv=android&lang=$tl&uuid=838743f8fe8b11eb9a030242ac130003&text=$txt")
                            val http: HttpURLConnection = url.openConnection() as HttpURLConnection
                            http.requestMethod = "POST"
                            http.doOutput = true
                            http.setRequestProperty("Content-Length", "0")

                            val a = http.inputStream

                            val br = BufferedReader(
                                InputStreamReader(
                                    a
                                )
                            )

                            val response = java.lang.StringBuilder()
                            var currentLine: String?

                            while (br.readLine().also { currentLine = it } != null) response.append(
                                currentLine
                            )

                            withContext(Dispatchers.Main) {
                                callback.invoke(
                                    JSONObject(response.toString()).optJSONArray("text")!!
                                        .getString(0)
                                )
                            }
                        }

                    } catch (e: Exception) {
                        callback.invoke("qtranslate error" +  e.message.toString())
                    }
                }
            }
            else -> {
                launch {
                    val apitran =
                        "https://translate.googleapis.com/translate_a/single?client=gtx&dt=t&dj=1&sl=auto"

                    try {

                        val request: Request = Request.Builder()
                            .url("$apitran&tl=$tl&q=${URLEncoder.encode(txt, "UTF-8")}")
                            .build()
                        val sb = StringBuilder()

                        withContext(Dispatchers.IO) {
                            val response = OkHttpClient().newCall(request).execute()
                            val arr = JSONObject(response.body!!.string()).getJSONArray("sentences")
                            for (i in 0 until arr.length()) {
                                sb.append(arr.getJSONObject(i).getString("trans"))
                            }
                        }

                        callback.invoke(sb.toString())
                    } catch (e: Exception) {
                        callback.invoke("qtranslate error" +  e.message.toString())
                    }
                }
            }
        }
    }

    fun detectLang(txt: String, callback: (String) -> Unit) {
        launch {
            try {
                withContext(Dispatchers.IO) {
                    val url =
                        URL("https://translate.yandex.net/api/v1/tr.json/detect?srv=android&text=$txt&uuid=838743f8fe8b11eb9a030242ac130003")
                    val http = url.openConnection() as HttpURLConnection
                    val a = http.inputStream

                    val br = BufferedReader(
                        InputStreamReader(
                            a
                        )
                    )

                    val response = java.lang.StringBuilder()
                    var currentLine: String?

                    while (br.readLine().also { currentLine = it } != null) response.append(
                        currentLine
                    )
                    withContext(Dispatchers.Main) {
                        callback.invoke(JSONObject(response.toString())["lang"].toString())
                    }
                }
            } catch (e: Exception) {
                callback.invoke("qtranslate error" +  e.message.toString())
            }
        }
    }
}