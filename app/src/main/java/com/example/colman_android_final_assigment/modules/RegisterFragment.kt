package com.example.colman_android_final_assigment.modules

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.colman_android_final_assigment.R
import com.example.colman_android_final_assigment.base.Resource
import com.example.colman_android_final_assigment.databinding.FragmentRegisterBinding
import com.example.colman_android_final_assigment.viewmodel.AuthViewModel

class RegisterFragment : Fragment() {
    private var _binding: FragmentRegisterBinding? = null
    private val binding get() = _binding!!
    private val viewModel: AuthViewModel by viewModels()
    private var imageUri: Uri? = null

    private val pickImage = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            imageUri = it
            binding.avatarImage.setImageURI(it)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRegisterBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.avatarImage.setOnClickListener {
            pickImage.launch("image/*")
        }

        binding.registerButton.setOnClickListener {
            val name = binding.nameEditText.text.toString()
            val email = binding.emailEditText.text.toString()
            val password = binding.passwordEditText.text.toString()
            val phone = binding.phoneEditText.text.toString()

            if (name.isNotEmpty() && email.isNotEmpty() && password.isNotEmpty() && phone.isNotEmpty()) {
                viewModel.register(name, email, password, phone, imageUri)
            } else {
                Toast.makeText(context, "Please fill all required fields", Toast.LENGTH_SHORT).show()
            }
        }

        binding.loginLink.setOnClickListener {
            findNavController().navigate(R.id.action_registerFragment_to_loginFragment)
        }

        viewModel.authState.observe(viewLifecycleOwner) { resource ->
            binding.registerProgressBar.visibility = if (resource is Resource.Loading) View.VISIBLE else View.GONE
            binding.registerButton.isEnabled = resource !is Resource.Loading

            when (resource) {
                is Resource.Success -> {
                    findNavController().navigate(R.id.action_registerFragment_to_feedFragment)
                }
                is Resource.Error -> {
                    Toast.makeText(context, resource.message, Toast.LENGTH_SHORT).show()
                }
                else -> {}
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
