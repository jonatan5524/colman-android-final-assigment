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

class LoginFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_login, container, false)

        val loginButton = view.findViewById<Button>(R.id.login_button)
        val registerLink = view.findViewById<TextView>(R.id.register_link)

        loginButton.setOnClickListener {
            findNavController().navigate(R.id.action_loginFragment_to_feedFragment)
        }

        registerLink.setOnClickListener {
            findNavController().navigate(R.id.action_loginFragment_to_registerFragment)
        }

        return view
    }
}