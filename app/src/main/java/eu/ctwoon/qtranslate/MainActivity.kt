package eu.ctwoon.qtranslate

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import android.text.Editable
import android.text.TextWatcher
import android.view.Menu
import android.view.MenuItem
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceManager
import com.google.android.material.bottomappbar.BottomAppBar
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.textfield.TextInputLayout
import eu.ctwoon.qtranslate.Provider.detectLang
import eu.ctwoon.qtranslate.Provider.translateText
import java.util.*
import kotlin.concurrent.schedule

class MainActivity : AppCompatActivity(), TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        super.onNewIntent(intent)
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

        appbar.menu.getItem(1).setOnMenuItemClickListener {
            notYet()
            return@setOnMenuItemClickListener true
        }

        appbar.menu.getItem(2).setOnMenuItemClickListener {
            notYet()
            return@setOnMenuItemClickListener true
        }

        findViewById<FloatingActionButton>(R.id.fab).setOnClickListener { getSpeechInput() }

        if (intent?.getStringExtra("CONTEXT_TEXT").toString() != "null") {
            onNewIntent(intent)
        }

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
                val a = s.toString()
                if (a == "")
                    return
                Timer().schedule(600) {
                    if (a == ed.text.toString()) {
                        translate(s.toString(), false)
                    }
                }
            }
        })
    }

    private fun translate(txt: String, native: Boolean) {
        val prefs: SharedPreferences = PreferenceManager
            .getDefaultSharedPreferences(this)
        val lang = if (native) prefs.getString("native", "en") else prefs.getString("lang", "en")
        val ed1 = findViewById<EditText>(R.id.ed1)
        // тут короче перевод сам ауе
        translateText(txt, lang, this) {
            if (it.contains("to resolve host")) {
                ed1.setText(getString(R.string.no_internet))
            } else if (it.contains("www2.deepl.com") || it.contains("Value <!DOCTYPE") || it.contains(
                    "translate.yandex"
                ) || it.contains("translate.googleapis")
            ) {
                ed1.setText(getString(R.string.rate_limit))
            } else {
                ed1.setText(it)
            }
        }
        // а тут получаем какого языка текст типо был
        detectLang(txt) {
            if (it.contains("to resolve host") || it.contains("yandex"))
                return@detectLang
            findViewById<TextInputLayout>(R.id.textField).hint =
                getString(R.string.input_text) + " · " + it
        }
        val a = getString(R.string.trans_text) + " · " + lang
        if (findViewById<TextInputLayout>(R.id.textField1).hint != a) {
            findViewById<TextInputLayout>(R.id.textField1).hint = a
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

        val a = prefs.getString("lang", "en")

        var mm = 0

        val listItems = arrayOf("en", "ru", "fr", "de", "it", "zh", "ja", "pt")

        for (i in listItems.indices) {
            if (a == listItems[i])
                mm = i
        }
        val mBuilder = AlertDialog.Builder(this@MainActivity)
        mBuilder.setTitle(getString(R.string.select_language))

        mBuilder.setSingleChoiceItems(listItems, mm) { dialogInterface, i ->
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
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
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
                val result = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                findViewById<EditText>(R.id.ed).setText(
                    Objects.requireNonNull(result)!![0]
                )
            }
        }
    }

    override fun onInit(status: Int) {}

    private fun tts() {
        val a = findViewById<EditText>(R.id.ed1).text
        val prefs: SharedPreferences = PreferenceManager
            .getDefaultSharedPreferences(this)
        tts!!.language = Locale(prefs.getString("lang", "en").toString())
        tts!!.speak(a, TextToSpeech.QUEUE_FLUSH, null, "")
    }

    private fun notYet() {
        Toast.makeText(this, "not yet implemented =(", Toast.LENGTH_SHORT).show()
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        val ed = findViewById<EditText>(R.id.ed)
        ed.requestFocus()
        ed.setText(intent?.getStringExtra("CONTEXT_TEXT"))
        translate(ed.text.toString(), true)
    }
}