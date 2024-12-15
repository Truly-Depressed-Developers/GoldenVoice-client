package com.example.udd_bitehack_client.recording

import android.content.Context
import android.util.Log
import java.io.BufferedReader
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStreamReader


class FileHelper(val context: Context) {
    fun writeFile(fileName: String, fileContent: ByteArray) {
        try {
            val fos: FileOutputStream = context.openFileOutput(fileName, Context.MODE_PRIVATE)
            fos.write(fileContent)
            fos.close()
        } catch (e: IOException) {
            e.printStackTrace()
            Log.e("FileHelper", "Error saving file: ${e.message}")
        }
    }

    fun getOutputStream(fileName: String): FileOutputStream? {
        return context.openFileOutput(fileName, Context.MODE_PRIVATE)
    }

    fun readFile(fileName: String): String?{
        try {
            val fis: FileInputStream = context.openFileInput(fileName)
            val reader = BufferedReader(InputStreamReader(fis))
            val builder = StringBuilder()
            var line: String?
            while ((reader.readLine().also { line = it }) != null) {
                builder.append(line).append("\n")
            }
            fis.close()
            val fileContent = builder.toString()
            return fileContent
        } catch (e: IOException) {
            e.printStackTrace()
        }

        return null
    }

    fun getInputStream(fileName: String): FileInputStream? {
        return context.openFileInput(fileName)
    }
}