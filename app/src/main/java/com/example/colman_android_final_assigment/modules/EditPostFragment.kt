package com.example.colman_android_final_assigment.modules

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.example.colman_android_final_assigment.R
import com.example.colman_android_final_assigment.base.Resource
import com.example.colman_android_final_assigment.databinding.FragmentEditPostBinding
import com.example.colman_android_final_assigment.viewmodel.EditPostViewModel
import com.squareup.picasso.Picasso

class EditPostFragment : Fragment() {
    private var _binding: FragmentEditPostBinding? = null
    private val binding get() = _binding!!
    private val viewModel: EditPostViewModel by viewModels()
    private val args: EditPostFragmentArgs by navArgs()
    private var imageUri: Uri? = null

    private val pickImage = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            imageUri = it
            binding.editPostImage.setImageURI(it)
        }
    }

    private val cities = arrayOf("Tel Aviv", "Haifa", "Jerusalem", "Beersheba", "Eilat")

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEditPostBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupCitySpinner()
        viewModel.setPostId(args.postId)
        observeViewModel()

        binding.changePhotoButton.setOnClickListener {
            pickImage.launch("image/*")
        }

        binding.savePostButton.setOnClickListener {
            val title = binding.editTitleEditText.text.toString()
            val description = binding.editDescriptionEditText.text.toString()
            val category = binding.editCategoryEditText.text.toString()
            val cityId = binding.editCitySpinner.selectedItemPosition

            if (title.isNotEmpty() && description.isNotEmpty()) {
                viewModel.savePost(title, description, category, cityId, imageUri)
            } else {
                Toast.makeText(context, "Please fill title and description", Toast.LENGTH_SHORT).show()
            }
        }

        binding.cancelEditPostButton.setOnClickListener {
            findNavController().popBackStack()
        }
    }

    private fun setupCitySpinner() {
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, cities)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.editCitySpinner.adapter = adapter
    }

    private fun observeViewModel() {
        viewModel.post.observe(viewLifecycleOwner) { post ->
            post?.let {
                binding.editTitleEditText.setText(it.title)
                binding.editDescriptionEditText.setText(it.description)
                binding.editCategoryEditText.setText(it.category)
                
                // Safety check for cityId range
                if (it.cityId in cities.indices) {
                    binding.editCitySpinner.setSelection(it.cityId)
                } else {
                    // Default to first item if ID is invalid (like 5000)
                    binding.editCitySpinner.setSelection(0)
                }

                if (it.imageUrl.isNotEmpty() && imageUri == null) {
                    Picasso.get().load(it.imageUrl).placeholder(R.drawable.bg_input).into(binding.editPostImage)
                }
            }
        }

        viewModel.updateState.observe(viewLifecycleOwner) { resource ->
            binding.editPostProgressBar.visibility = if (resource is Resource.Loading) View.VISIBLE else View.GONE
            binding.savePostButton.isEnabled = resource !is Resource.Loading

            when (resource) {
                is Resource.Success -> {
                    Toast.makeText(context, "Post updated successfully", Toast.LENGTH_SHORT).show()
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
