package eu.ctwoon.qtranslate

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.preference.PreferenceManager
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomappbar.BottomAppBar
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.textfield.TextInputLayout
import eu.ctwoon.qtranslate.provider.Provider.deeplTranslate
import eu.ctwoon.qtranslate.provider.Provider.detectLang
import eu.ctwoon.qtranslate.provider.Provider.googleTranslate
import eu.ctwoon.qtranslate.provider.Provider.yandexTranslate
import java.util.*
import kotlin.concurrent.schedule


class MainActivity : AppCompatActivity(), TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(findViewById(R.id.toolbar))

        val ed = findViewById<EditText>(R.id.ed)

        tts = TextToSpeech(this, this)

        val appbar = findViewById<BottomAppBar>(R.id.bottom_app_bar)

        appbar.replaceMenu(R.menu.bottom)

        appbar.menu.getItem(0).setOnMenuItemClickListener {
            tts()
            return@setOnMenuItemClickListener true
        }

        findViewById<FloatingActionButton>(R.id.fab).setOnClickListener { getSpeechInput() }

        findViewById<EditText>(R.id.ed).addTextChangedListener(object : TextWatcher {

            override fun afterTextChanged(s: Editable) {
            }

            override fun beforeTextChanged(
                s: CharSequence, start: Int,
                count: Int, after: Int
            ) {
            }

            override fun onTextChanged(
                s: CharSequence, start: Int,
                before: Int, count: Int
            ) {
                var a = s.toString()
                if (a == "")
                    return
                Timer().schedule(600) {
                    if (a == ed.text.toString()) {
                        translate(s.toString())
                    }
                }
            }
        })
    }

    private fun translate(txt: String) {
        val prefs: SharedPreferences = PreferenceManager
            .getDefaultSharedPreferences(this)
        var a = prefs.getInt("trans", 2)
        var lang = prefs.getString("lang", "en")
        var ed1 = findViewById<EditText>(R.id.ed1)
        // тут короче перевод сам ауе
        when (a) {
            2 -> deeplTranslate(txt, lang) { txt ->
                ed1.setText(txt)
            }
            1 -> yandexTranslate(txt, lang) { txt ->
                ed1.setText(txt)
            }
            else -> googleTranslate(txt, lang) { txt ->
                ed1.setText(txt)
            }
        }
        // а тут получаем какого языка текст типо был
        detectLang(txt) { txt ->
            findViewById<TextInputLayout>(R.id.textField).hint =
                getString(R.string.input_text) + " · " + txt
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        val inflater = menuInflater
        inflater.inflate(R.menu.menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle item selection
         when (item.itemId) {
            R.id.lang -> {
                selectLanguageDialog()
            }
            R.id.engine -> {
                selectEngineDialog()
            }
            R.id.settings -> {
                startActivity(Intent(this, Settings::class.java))
            }

            else -> super.onOptionsItemSelected(item)
        }
        return true
    }

    private fun selectEngineDialog() {
        val prefs: SharedPreferences = PreferenceManager
            .getDefaultSharedPreferences(this)
        val ed: SharedPreferences.Editor = prefs.edit()

        val a = prefs.getInt("trans", 2)

        val listItems = arrayOf("Google", "Yandex", "Deepl")
        val mBuilder = AlertDialog.Builder(this@MainActivity)
        mBuilder.setTitle(getString(R.string.select_engine))
        mBuilder.setSingleChoiceItems(listItems, a) { dialogInterface, i ->
            ed.putInt("trans", i)
            ed.apply()
            dialogInterface.dismiss()
        }

        mBuilder.setNeutralButton(getString(R.string.cancel)) { dialog, _ ->
            dialog.cancel()
        }

        mBuilder.setPositiveButton("OK") { dialog, _ ->
            dialog.cancel()
        }

        val mDialog = mBuilder.create()
        mDialog.show()
    }

    private fun selectLanguageDialog() {
        val prefs: SharedPreferences = PreferenceManager
            .getDefaultSharedPreferences(this)
        val ed: SharedPreferences.Editor = prefs.edit()

        val listItems = arrayOf("en", "ru", "fr", "de", "it", "zh", "jp")
        val mBuilder = AlertDialog.Builder(this@MainActivity)
        mBuilder.setTitle(getString(R.string.select_language))
        mBuilder.setSingleChoiceItems(listItems, -1) { dialogInterface, i ->
            ed.putString("lang", listItems[i])
            ed.apply()
            dialogInterface.dismiss()
        }

        mBuilder.setNeutralButton(getString(R.string.cancel)) { dialog, _ ->
            dialog.cancel()
        }

        mBuilder.setPositiveButton("OK") { dialog, _ ->
            dialog.cancel()
        }

        val mDialog = mBuilder.create()
        mDialog.show()
    }

    private fun getSpeechInput() {
        var intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
        intent.putExtra(
            RecognizerIntent.EXTRA_LANGUAGE_MODEL,
            RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
        )
        intent.putExtra(
            RecognizerIntent.EXTRA_LANGUAGE,
            Locale.getDefault()
        )
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak to text")

        try {
            startActivityForResult(intent, 10)
        } catch (e: Exception) {
            Toast
                .makeText(
                    this, " " + e.message.toString(),
                    Toast.LENGTH_SHORT
                )
                .show()
        }
    }

    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int, data: Intent?
    ) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 10) {
            if (resultCode == RESULT_OK && data != null) {
                var result = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                findViewById<EditText>(R.id.ed).setText(
                    Objects.requireNonNull(result)!![0]
                )
            }
        }
    }

    override fun onInit(status: Int) {}

    fun tts() {
        var a = findViewById<EditText>(R.id.ed1).text
        val prefs: SharedPreferences = PreferenceManager
            .getDefaultSharedPreferences(this)
        tts!!.language = Locale(prefs.getString("lang", "en"))
        tts!!.speak(a, TextToSpeech.QUEUE_FLUSH, null, "")
    }

    fun notYet() {
        Toast.makeText(this, "not yet implemented =(", Toast.LENGTH_SHORT).show()
    }
}