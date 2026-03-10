package com.example.colman_android_final_assigment.modules

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.colman_android_final_assigment.R
import com.example.colman_android_final_assigment.base.Resource
import com.example.colman_android_final_assigment.databinding.FragmentProfileBinding
import com.example.colman_android_final_assigment.viewmodel.ProfileViewModel
import com.squareup.picasso.Picasso

class ProfileFragment : Fragment() {
    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!
    private val viewModel: ProfileViewModel by viewModels()
    private var currentAvatarUrl: String = ""

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.userDetails?.observe(viewLifecycleOwner) { resource ->
            binding.profileProgressBar.visibility = if (resource is Resource.Loading) View.VISIBLE else View.GONE
            
            if (resource is Resource.Success) {
                val user = resource.data
                binding.profileName.text = user.name
                binding.profileEmail.text = user.email
                currentAvatarUrl = user.avatarUrl
                if (user.avatarUrl.isNotEmpty()) {
                    Picasso.get().load(user.avatarUrl).placeholder(R.drawable.ic_person_placeholder).into(binding.profileImage)
                }
            }
        }

        binding.editProfileLink.setOnClickListener {
            val name = binding.profileName.text.toString()
            val action = ProfileFragmentDirections.actionProfileFragmentToEditProfileFragment(
                name = name,
                imgUrl = currentAvatarUrl
            )
            findNavController().navigate(action)
        }

        binding.logoutButton.setOnClickListener {
            viewModel.logout()
            findNavController().navigate(R.id.action_profileFragment_to_loginFragment)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
