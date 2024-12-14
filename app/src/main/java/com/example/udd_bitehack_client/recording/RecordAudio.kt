package com.example.udd_bitehack_client.recording

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.AsyncTask
import android.os.Environment
import android.util.Log
import androidx.core.app.ActivityCompat
import com.example.udd_bitehack_client.AUDIO_RECORDER_FILE_EXT_WAV
import com.example.udd_bitehack_client.AUDIO_RECORDER_FOLDER
import com.example.udd_bitehack_client.AUDIO_RECORDER_TEMP_FILE
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException


class RecordAudio(val context: Context, var os: FileOutputStream?, var endedCallback: (fName: String) -> Unit) : AsyncTask<Void?, Double?, Void?>() {
    companion object {
        const val RECORDER_BPP: Int = 16
        val TAG = "RecordAudio"
        val RECORD_AUDIO_PERMISSION_REQUEST_CODE = 4001
    }

    var bufferSize: Int = 0
    var frequency: Int = 44100 //8000;
    var channelConfiguration: Int = AudioFormat.CHANNEL_IN_MONO
    var audioEncoding: Int = AudioFormat.ENCODING_PCM_16BIT
    var started: Boolean = false

    var threshold: Short = 5000
    val silenceTimeMax: Short = 20
    var silenceCounter: Short = 0


    override fun doInBackground(vararg arg0: Void?): Void? {
        Log.w(TAG, "doInBackground")

        val filename = tempFilename

        try {
            os = FileOutputStream(filename)
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
        }


        bufferSize = AudioRecord.getMinBufferSize(
            frequency,
            channelConfiguration, audioEncoding
        )

        val audioRecord = AudioRecord(
            MediaRecorder.AudioSource.MIC, frequency,
            channelConfiguration, audioEncoding, bufferSize
        )

            if (ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.RECORD_AUDIO
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                // Request the permission
                ActivityCompat.requestPermissions(
                    context as Activity, // Assuming 'context' is an Activity
                    arrayOf(Manifest.permission.RECORD_AUDIO),
                    RECORD_AUDIO_PERMISSION_REQUEST_CODE
                )
                return null
            }

            startRecording(audioRecord)

            return null
        } //fine di doInBackground

        private fun startRecording(audioRecord: AudioRecord) {
            try{
                started = true
            val buffer = ShortArray(bufferSize)

            audioRecord.startRecording()
            silenceCounter = 0

            while (started) {
                val bufferReadResult = audioRecord.read(buffer, 0, bufferSize)
                if (AudioRecord.ERROR_INVALID_OPERATION != bufferReadResult) {
                    //check signal
                    //put a threshold
                    val foundPeak = searchThreshold(buffer, threshold)
                    Log.d(TAG, "foundPeak: $foundPeak")
                    if (foundPeak > -1) { //found signal
                        //record signal
                        val byteBuffer = ShortToByte(buffer, bufferReadResult)
                        try {
                            os?.write(byteBuffer)
                        } catch (e: IOException) {
                            e.printStackTrace()
                        }
                        silenceCounter = 0
                    } else { //count the time
                        silenceCounter++
                        Log.d(TAG, "silenceCounter: $silenceCounter")
                        if(silenceCounter > silenceTimeMax){
                            started=false
                            Log.d(TAG, "silence time ended ending recording")
                        }
                    }


                    //show results
                    //here, with publichProgress function, if you calculate the total saved samples,
                    //you can optionally show the recorded file length in seconds:      publishProgress(elsapsedTime,0);
                }
            }

            audioRecord.stop()


            //close file
            try {
                os?.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }

            copyWaveFile(tempFilename, filename)
            deleteTempFile()
            Log.d(TAG, "Audio recorded successfully $tempFilename, $filename")
            endedCallback(filename)
        } catch (t: Throwable) {
            t.message?.let { Log.e(TAG, it) }
            Log.e(TAG, "Recording Failed")
        }
    }

    fun ShortToByte(input: ShortArray, elements: Int): ByteArray {
        var short_index: Int
        val iterations = elements //input.length;
        val buffer = ByteArray(iterations * 2)

        var byte_index = 0
        short_index = byte_index

        while ( /*NOP*/short_index != iterations /*NOP*/) {
            buffer[byte_index] = (input[short_index].toInt() and 0x00FF).toByte()
            buffer[byte_index + 1] = ((input[short_index].toInt() and 0xFF00) shr 8).toByte()

            ++short_index
            byte_index += 2
        }

        return buffer
    }


    fun searchThreshold(arr: ShortArray, thr: Short): Int {
        val arrLen = arr.size
        var peakIndex = 0
        while (peakIndex < arrLen) {
            if ((arr[peakIndex] >= thr) || (arr[peakIndex] <= -thr)) {
                //se supera la soglia, esci e ritorna peakindex-mezzo kernel.

                return peakIndex
            }
            peakIndex++
        }
        return -1 //not found
    }

    val filename: String
        /*
               @Override
               protected void onProgressUpdate(Double... values) {
                   DecimalFormat sf = new DecimalFormat("000.0000");
                   elapsedTimeTxt.setText(sf.format(values[0]));

               }
               */
        get() {
            val filepath = Environment.getExternalStorageDirectory().path
            val file = File(filepath, AUDIO_RECORDER_FOLDER)

            if (!file.exists()) {
                file.mkdirs()
            }

            return (file.absolutePath + "/" + System.currentTimeMillis() + AUDIO_RECORDER_FILE_EXT_WAV)
        }


    private val tempFilename: String
        get() {
            val filepath = Environment.getExternalStorageDirectory().path
            val file = File(filepath, AUDIO_RECORDER_FOLDER)

            if (!file.exists()) {
                file.mkdirs()
            }

            val tempFile = File(filepath, AUDIO_RECORDER_TEMP_FILE)

            if (tempFile.exists()) tempFile.delete()

            return (file.absolutePath + "/" + AUDIO_RECORDER_TEMP_FILE)
        }


    private fun deleteTempFile() {
        val file = File(tempFilename)

        file.delete()
    }

    private fun copyWaveFile(inFilename: String, outFilename: String) {
        var `in`: FileInputStream? = null
        var out: FileOutputStream? = null
        var totalAudioLen: Long = 0
        var totalDataLen = totalAudioLen + 36
        val longSampleRate = frequency.toLong()
        val channels = 1
        val byteRate: Long = (RecordAudio.RECORDER_BPP * frequency * channels / 8).toLong()

        val data = ByteArray(bufferSize)

        try {
            `in` = FileInputStream(inFilename)
            out = FileOutputStream(outFilename)
            totalAudioLen = `in`.channel.size()
            totalDataLen = totalAudioLen + 36


            WriteWaveFileHeader(
                out, totalAudioLen, totalDataLen,
                longSampleRate, channels, byteRate
            )

            while (`in`.read(data) != -1) {
                out.write(data)
            }

            `in`.close()
            out.close()
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    @Throws(IOException::class)
    private fun WriteWaveFileHeader(
        out: FileOutputStream, totalAudioLen: Long,
        totalDataLen: Long, longSampleRate: Long, channels: Int,
        byteRate: Long
    ) {
        val header = ByteArray(44)

        header[0] = 'R'.code.toByte() // RIFF/WAVE header
        header[1] = 'I'.code.toByte()
        header[2] = 'F'.code.toByte()
        header[3] = 'F'.code.toByte()
        header[4] = (totalDataLen and 0xffL).toByte()
        header[5] = ((totalDataLen shr 8) and 0xffL).toByte()
        header[6] = ((totalDataLen shr 16) and 0xffL).toByte()
        header[7] = ((totalDataLen shr 24) and 0xffL).toByte()
        header[8] = 'W'.code.toByte()
        header[9] = 'A'.code.toByte()
        header[10] = 'V'.code.toByte()
        header[11] = 'E'.code.toByte()
        header[12] = 'f'.code.toByte() // 'fmt ' chunk
        header[13] = 'm'.code.toByte()
        header[14] = 't'.code.toByte()
        header[15] = ' '.code.toByte()
        header[16] = 16 // 4 bytes: size of 'fmt ' chunk
        header[17] = 0
        header[18] = 0
        header[19] = 0
        header[20] = 1 // format = 1
        header[21] = 0
        header[22] = channels.toByte()
        header[23] = 0
        header[24] = (longSampleRate and 0xffL).toByte()
        header[25] = ((longSampleRate shr 8) and 0xffL).toByte()
        header[26] = ((longSampleRate shr 16) and 0xffL).toByte()
        header[27] = ((longSampleRate shr 24) and 0xffL).toByte()
        header[28] = (byteRate and 0xffL).toByte()
        header[29] = ((byteRate shr 8) and 0xffL).toByte()
        header[30] = ((byteRate shr 16) and 0xffL).toByte()
        header[31] = ((byteRate shr 24) and 0xffL).toByte()
        header[32] = (channels * 16 / 8).toByte() // block align
        header[33] = 0
        header[34] = Companion.RECORDER_BPP.toByte() // bits per sample
        header[35] = 0
        header[36] = 'd'.code.toByte()
        header[37] = 'a'.code.toByte()
        header[38] = 't'.code.toByte()
        header[39] = 'a'.code.toByte()
        header[40] = (totalAudioLen and 0xffL).toByte()
        header[41] = ((totalAudioLen shr 8) and 0xffL).toByte()
        header[42] = ((totalAudioLen shr 16) and 0xffL).toByte()
        header[43] = ((totalAudioLen shr 24) and 0xffL).toByte()

        out.write(header, 0, 44)
    }
}