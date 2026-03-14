package com.example.colman_android_final_assigment.modules

import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
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
import com.example.colman_android_final_assigment.utils.Constants
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

    private val cityNames = Constants.getCityNames()

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
        setupTextWatchers()
        viewModel.setPostId(args.postId)
        observeViewModel()

        binding.changePhotoButton.setOnClickListener {
            pickImage.launch("image/*")
        }

        binding.savePostButton.setOnClickListener {
            validateAndSave()
        }

        binding.cancelEditPostButton.setOnClickListener {
            findNavController().popBackStack()
        }
    }

    private fun setupTextWatchers() {
        val watcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                binding.editTitleLayout.error = null
                binding.editDescriptionLayout.error = null
                binding.editCategoryLayout.error = null
            }
            override fun afterTextChanged(s: Editable?) {}
        }
        binding.editTitleEditText.addTextChangedListener(watcher)
        binding.editDescriptionEditText.addTextChangedListener(watcher)
        binding.editCategoryEditText.addTextChangedListener(watcher)
    }

    private fun validateAndSave() {
        val title = binding.editTitleEditText.text.toString().trim()
        val description = binding.editDescriptionEditText.text.toString().trim()
        val category = binding.editCategoryEditText.text.toString().trim()
        val selectedCityName = binding.editCitySpinner.selectedItem.toString()
        val cityId = Constants.getCityIdByName(selectedCityName)

        var isValid = true

        if (title.isEmpty()) {
            binding.editTitleLayout.error = getString(R.string.error_empty_title)
            isValid = false
        }
        if (description.isEmpty()) {
            binding.editDescriptionLayout.error = getString(R.string.error_empty_description)
            isValid = false
        }
        if (category.isEmpty()) {
            binding.editCategoryLayout.error = getString(R.string.error_empty_category)
            isValid = false
        }

        if (isValid) {
            viewModel.savePost(title, description, category, cityId, imageUri)
        }
    }

    private fun setupCitySpinner() {
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, cityNames)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.editCitySpinner.adapter = adapter
    }

    private fun observeViewModel() {
        viewModel.post.observe(viewLifecycleOwner) { post ->
            post?.let {
                binding.editTitleEditText.setText(it.title)
                binding.editDescriptionEditText.setText(it.description)
                binding.editCategoryEditText.setText(it.category)
                
                val currentCityName = Constants.getCityNameById(it.cityId)
                val cityIndex = cityNames.indexOf(currentCityName)
                if (cityIndex != -1) {
                    binding.editCitySpinner.setSelection(cityIndex)
                } else {
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
