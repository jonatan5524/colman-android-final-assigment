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
import androidx.navigation.fragment.navArgs
import com.example.colman_android_final_assigment.R
import com.example.colman_android_final_assigment.base.Resource
import com.example.colman_android_final_assigment.databinding.FragmentEditProfileBinding
import com.example.colman_android_final_assigment.viewmodel.ProfileViewModel
import com.squareup.picasso.Picasso

class EditProfileFragment : Fragment() {
    private var _binding: FragmentEditProfileBinding? = null
    private val binding get() = _binding!!
    private val viewModel: ProfileViewModel by viewModels()
    private val args: EditProfileFragmentArgs by navArgs()
    private var imageUri: Uri? = null

    private val pickImage = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            imageUri = it
            binding.editProfileImage.setImageURI(it)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEditProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.editNameEditText.setText(args.name)
        if (args.imgUrl.isNotEmpty()) {
            Picasso.get().load(args.imgUrl).placeholder(R.drawable.ic_person_placeholder).into(binding.editProfileImage)
        }

        binding.changePhotoButton.setOnClickListener {
            pickImage.launch("image/*")
        }

        binding.saveProfileButton.setOnClickListener {
            val name = binding.editNameEditText.text.toString()
            if (name.isNotEmpty()) {
                viewModel.updateProfile(name, imageUri)
            } else {
                Toast.makeText(context, "Name cannot be empty", Toast.LENGTH_SHORT).show()
            }
        }

        viewModel.updateState.observe(viewLifecycleOwner) { resource ->
            binding.saveProfileProgressBar.visibility = if (resource is Resource.Loading) View.VISIBLE else View.GONE
            binding.saveProfileButton.isEnabled = resource !is Resource.Loading

            when (resource) {
                is Resource.Success -> {
                    Toast.makeText(context, "Profile updated", Toast.LENGTH_SHORT).show()
                    findNavController().popBackStack()
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
