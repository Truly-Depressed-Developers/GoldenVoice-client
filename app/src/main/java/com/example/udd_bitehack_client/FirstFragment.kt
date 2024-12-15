package com.example.udd_bitehack_client

import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.udd_bitehack_client.api_communication.ApiCommunicationController
import com.example.udd_bitehack_client.databinding.FragmentFirstBinding
import com.example.udd_bitehack_client.playing.TTSController
import com.example.udd_bitehack_client.recording.FileHelper
import com.example.udd_bitehack_client.recording.RecordAudio
import com.example.udd_bitehack_client.recording.RecordAudio.Companion.RECORD_AUDIO_PERMISSION_REQUEST_CODE
import java.io.ByteArrayOutputStream
import java.io.FileOutputStream


/**
 * A simple [Fragment] subclass as the default destination in the navigation.
 */
class FirstFragment : Fragment() {
    private val tag = "PhoneAssistant"
    private var _binding: FragmentFirstBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    var started: Boolean = false
    var recordTask: RecordAudio? = null
    var os: FileOutputStream? = null

    lateinit var fileHelper: FileHelper
    private val apiController = ApiCommunicationController()
    lateinit var ttsController: TTSController

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = FragmentFirstBinding.inflate(inflater, container, false)
        return binding.root

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.recordBtn.setOnClickListener {
            handleRecordButtonClick()
        }
        binding.submitBtn.setOnClickListener {
            submitClicked()
        }

        fileHelper = FileHelper(requireContext())
        ttsController = TTSController(requireContext())
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun submitClicked(){
        val text = binding.textInput.text.toString()
        if(text.isEmpty()){
            return
        }

        Log.d(tag, "button submit clicked, text: $text")
        addMessageRequest(text)
        addMessageResponse("Ok. Processing...")
        binding.textInput.text.clear()
    }

    private fun addMessageRequest(text: String){
        val textView = createRequestTextView()
        textView.text = text
    }

    private fun createRequestTextView(): TextView{
        val textView = TextView(this.context)
        textView.setBackgroundResource(R.drawable.text_request_styles)
        textView.setTextColor(ContextCompat.getColor(this.requireContext(), R.color.tekst))
        textView.setPadding(10, 10, 10, 10) // Left, top, right, bottom padding
        textView.textSize = 18f

        binding.messagesContainer.addView(textView)

        val params = textView.layoutParams as LinearLayout.LayoutParams
        params.gravity = Gravity.END
        params.leftMargin = 1
        params.width = LinearLayout.LayoutParams.WRAP_CONTENT
        textView.layoutParams = params
        return textView
    }

    private fun addMessageResponse(text: String){
        val textView = createResponseTextView()
        textView.text = text
    }

    private fun createResponseTextView(): TextView{
        val textView = TextView(this.context)
        textView.setBackgroundResource(R.drawable.text_response_styles)
        textView.setTextColor(ContextCompat.getColor(this.requireContext(), R.color.tekst))
        textView.setPadding(10, 10, 10, 10) // Left, top, right, bottom padding
        textView.textSize = 18f

        binding.messagesContainer.addView(textView)

        val params = textView.layoutParams as LinearLayout.LayoutParams
        params.gravity = Gravity.START
        params.rightMargin = 1
        params.width = LinearLayout.LayoutParams.WRAP_CONTENT
        textView.layoutParams = params
        return textView
    }

    // recording

    private fun handleRecordButtonClick(){
        Log.d(tag, "button record clicked")
        if(started){
            stopAquisition()
            // change recordButton background color to red
            binding.recordBtn.clearColorFilter()
        } else {
            startAquisition()
            // clear recordButton background color
            binding.recordBtn.setColorFilter(ContextCompat.getColor(requireContext(), R.color.red))
        }
    }

    private fun onRecordingfinished(fName: String){
        Log.d(tag, "recording finished")
        started = false
        binding.recordBtn.clearColorFilter()
        // get file under fName
        Log.d(tag, "file path: $fName")
        val txt = fileHelper.readFile(fName)
        val stream = fileHelper.getOutputStream(fName)
        Log.d(tag, "file: $txt ${stream?.channel?.size()} ${stream?.fd?.valid()}")

        val baos = ByteArrayOutputStream()
        if (stream != null) {
            baos.writeTo(stream)
        }

        apiController.uploadFileAndGetResponse(baos.toByteArray()){
            res, req ->
            activity?.runOnUiThread {
                addMessageRequest(req)
                addMessageResponse(res)
                ttsController.speakText(res)
            }
        }
        stream?.close()
    }

    fun stopAquisition() {
        Log.w(tag, "stopAquisition")
        if (started) {
            started = false
            recordTask?.started = false
//            recordTask?.cancel(true)
        }
    }

    fun startAquisition() {
        Log.w(tag, "startAquisition")
        val handler: Handler = Handler()
        handler.postDelayed(Runnable {
            //elapsedTime=0;
            started = true
            recordTask = RecordAudio(requireContext(), os){
                f -> onRecordingfinished(f)
            }
            recordTask!!.execute()
            //startButton.setText("RESET");
        }, 500)
    }

    @Deprecated("Deprecated in Java")
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == RECORD_AUDIO_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startAquisition()
            } else {
                addMessageResponse("Audio recording permission denied.")
            }
        }
    }
}