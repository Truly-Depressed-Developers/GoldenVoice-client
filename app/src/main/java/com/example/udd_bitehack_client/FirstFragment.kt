package com.example.udd_bitehack_client

import android.icu.text.ListFormatter.Width
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.view.setPadding
import com.example.udd_bitehack_client.databinding.FragmentFirstBinding

/**
 * A simple [Fragment] subclass as the default destination in the navigation.
 */
class FirstFragment : Fragment() {
    private val tag = "PhoneAssistant"
    private var _binding: FragmentFirstBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

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
            Log.d(tag, "button record clicked")
        }
        binding.submitBtn.setOnClickListener {
            submitClicked()
        }
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
        textView.setTextColor(ContextCompat.getColor(this.requireContext(), R.color.white))
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
        textView.setTextColor(ContextCompat.getColor(this.requireContext(), R.color.white))
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
}