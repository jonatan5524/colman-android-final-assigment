package com.example.colman_android_final_assigment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController

class ProfileFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_profile, container, false)

        val editProfileLink = view.findViewById<TextView>(R.id.edit_profile_link)
        val logoutButton = view.findViewById<Button>(R.id.logout_button)

        editProfileLink.setOnClickListener {
            val action = ProfileFragmentDirections.actionProfileFragmentToEditProfileFragment(
                name = view.findViewById<TextView>(R.id.profile_name).text.toString(),
                imgUrl = "" // Default or current image URL
            )
            findNavController().navigate(action)
        }

        logoutButton.setOnClickListener {
            findNavController().navigate(R.id.loginFragment)
        }

        return view
    }
}
