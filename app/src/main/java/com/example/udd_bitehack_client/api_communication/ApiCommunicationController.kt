package com.example.udd_bitehack_client.api_communication

import android.util.Log
import com.example.udd_bitehack_client.SERVER_PATH
import com.example.udd_bitehack_client.SERVER_URL
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.Response
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStream
import java.net.URL

class ApiCommunicationController {
    val client = OkHttpClient()

    fun uploadFileAndGetResponse(stream: ByteArray, callback: (res: String, req: String) -> Unit) {
        val fileRequestBody = RequestBody.create("audio/wave".toMediaTypeOrNull(), stream)

        val request = Request.Builder()
            .url("$SERVER_URL$SERVER_PATH")
            .post(fileRequestBody)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                // Handle the error
                e.printStackTrace()
                Log.d("Response","Failed to upload file: ${e.message}")
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    // Parse the JSON response
                    val jsonResponse = response.body?.string()
                    val jsonObject = JSONObject(jsonResponse ?: "")
                    val responseString = jsonObject.optString("response")
                    val requestString = jsonObject.optString("request")

                    Log.d("Response", "Response from server: $jsonResponse")
                    Log.d("Response", responseString)
                    println("Request sent: $requestString")
                    Log.d("Request", requestString)
                    callback(responseString, requestString)
                } else {
                    Log.d("Request", "Server error: ${response.code}")
                }
            }
        })
    }
}