package com.example.udd_bitehack_client.playing

import android.content.Context
import android.speech.tts.TextToSpeech
import java.util.Locale

class TTSController(context: Context) : TextToSpeech.OnInitListener {
    private val tts = TextToSpeech(context, this)

    fun speakText(text: String) {
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts.setLanguage(Locale.ROOT)

            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                println("TTS: The language is not supported!")
            }
        } else {
            println("TTS Initialization failed!")
        }
    }

    fun onDestroy() {
        tts.stop()
        tts.shutdown()
    }
}