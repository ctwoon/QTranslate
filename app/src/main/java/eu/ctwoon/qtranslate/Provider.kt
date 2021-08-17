package eu.ctwoon.qtranslate.provider

import kotlinx.coroutines.*
import okhttp3.Dispatcher
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

object Provider : CoroutineScope by MainScope() {

    @JvmStatic
    fun googleTranslate(txt: String?, tl: String?, callback: (String) -> Unit) {
        launch {
            val api_translate_url =
                "https://translate.googleapis.com/translate_a/single?client=gtx&dt=t&dj=1&sl=auto"

            try {

                val request: Request = Request.Builder()
                    .url("$api_translate_url&tl=$tl&q=${URLEncoder.encode(txt, "UTF-8")}").build()
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
                callback.invoke(e.message.toString())
            }
        }
    }

    fun yandexTranslate(txt: String, lang: String?, callback: (String) -> Unit) {

        launch {
            try {
                withContext(Dispatchers.IO) {
                    val url =
                        URL("https://translate.yandex.net/api/v1/tr.json/translate?srv=android&lang=$lang&uuid=838743f8fe8b11eb9a030242ac130003&text=$txt")
                    val http: HttpURLConnection = url.openConnection() as HttpURLConnection
                    http.requestMethod = "POST"
                    http.doOutput = true
                    http.setRequestProperty("Content-Length", "0")

                    var a = http.inputStream

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
                            JSONObject(response.toString()).optJSONArray("text")!!.getString(0)
                        )
                    }
                }

            } catch (e: Exception) {
                callback.invoke(e.message.toString())
            }
        }
    }

    fun deeplTranslate(txt: String, lang: String?, callback: (String) -> Unit) {
        launch {
            try {
                withContext(Dispatchers.IO) {
                    val url = URL("https://www2.deepl.com/jsonrpc")
                    val http = url.openConnection() as HttpURLConnection
                    http.requestMethod = "POST"
                    http.doOutput = true
                    http.setRequestProperty("Content-type", "application/json")

                    val data =
                        "{\"jsonrpc\":\"2.0\",\"method\": \"LMT_handle_jobs\",\"params\":{\"jobs\":[{\"kind\":\"default\",\"raw_en_sentence\":\"$txt\",\"raw_en_context_before\":[],\"raw_en_context_after\":[],\"preferred_num_beams\":4,\"quality\":\"fast\"}],\"lang\":{\"source_lang_user_selected\":\"auto\",\"target_lang\":\"$lang\"},\"priority\":-1,\"commonJobParams\":{},\"timestamp\":1602790014301}}"

                    val out: ByteArray = data.toByteArray(StandardCharsets.UTF_8)

                    val stream: OutputStream = http.outputStream

                    stream.write(out)

                    var a = http.inputStream

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

                    var aa =
                        JSONObject(JSONArray(JSONObject(JSONArray(JSONObject(JSONObject(response.toString())["result"].toString())["translations"].toString())[0].toString())["beams"].toString())[0].toString())["postprocessed_sentence"].toString()

                    withContext(Dispatchers.Main) {
                        callback.invoke(aa)
                    }
                }
            } catch (e: Exception) {
                callback.invoke(e.message.toString())
            }
        }
    }
    fun detectLang(txt: String, callback: (String) -> Unit) {
        launch {
            try {
                withContext(Dispatchers.IO) {
                    val url = URL("https://translate.yandex.net/api/v1/tr.json/detect?srv=android&text=$txt&uuid=838743f8fe8b11eb9a030242ac130003")
                    val http = url.openConnection() as HttpURLConnection
                    var a = http.inputStream

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
            }
            catch(e: Exception) {
                callback.invoke(e.message.toString())
            }
        }
    }
}