package com.example.colman_android_final_assigment.modules

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.colman_android_final_assigment.R

class RegisterFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_register, container, false)

        val registerButton = view.findViewById<Button>(R.id.register_button)
        val loginLink = view.findViewById<TextView>(R.id.login_link)

        registerButton.setOnClickListener {
            findNavController().navigate(R.id.action_registerFragment_to_feedFragment)
        }

        loginLink.setOnClickListener {
            findNavController().navigate(R.id.action_registerFragment_to_loginFragment)
        }

        return view
    }
}