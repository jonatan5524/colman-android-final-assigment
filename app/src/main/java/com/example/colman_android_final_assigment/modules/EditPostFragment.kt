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
import com.example.colman_android_final_assigment.model.Category
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

    private var cityList: List<Pair<Int, String>> = emptyList()
    private var categoryList: List<Category> = emptyList()
    private var selectedCategoryId: String? = null
    private var selectedCityId: Int? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEditPostBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
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
        binding.editCategoryAutocomplete.addTextChangedListener(watcher)
    }

    private fun validateAndSave() {
        val title = binding.editTitleEditText.text.toString().trim()
        val description = binding.editDescriptionEditText.text.toString().trim()

        var isValid = true

        if (title.isEmpty()) {
            binding.editTitleLayout.error = getString(R.string.error_empty_title)
            isValid = false
        }
        if (description.isEmpty()) {
            binding.editDescriptionLayout.error = getString(R.string.error_empty_description)
            isValid = false
        }
        val categoryId = selectedCategoryId
        if (categoryId == null) {
            binding.editCategoryLayout.error = getString(R.string.error_empty_category)
            isValid = false
        } else {
            binding.editCategoryLayout.error = null
        }

        val cityId = selectedCityId
        if (cityId == null) {
            binding.editCityLayout.error = "City cannot be empty"
            isValid = false
        } else {
            binding.editCityLayout.error = null
        }

        if (isValid) {
            viewModel.savePost(title, description, categoryId!!, cityId!!, imageUri)
        }
    }

    private fun observeViewModel() {
        viewModel.categories.observe(viewLifecycleOwner) { categories ->
            categoryList = categories
            val names = categories.map { it.name }
            val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, names)
            binding.editCategoryAutocomplete.setAdapter(adapter)

            binding.editCategoryAutocomplete.setOnItemClickListener { _, _, position, _ ->
                val selectedName = binding.editCategoryAutocomplete.adapter.getItem(position) as String
                selectedCategoryId = categories.firstOrNull { it.name == selectedName }?.id
            }

            binding.editCategoryAutocomplete.setOnClickListener {
                if (binding.editCategoryAutocomplete.text.isEmpty()) {
                    adapter.filter.filter(null) {
                        binding.editCategoryAutocomplete.post { binding.editCategoryAutocomplete.showDropDown() }
                    }
                } else {
                    binding.editCategoryAutocomplete.showDropDown()
                }
            }
            binding.editCategoryAutocomplete.setOnFocusChangeListener { _, hasFocus -> 
                if (hasFocus) {
                    if (binding.editCategoryAutocomplete.text.isEmpty()) {
                        adapter.filter.filter(null) {
                            binding.editCategoryAutocomplete.post { binding.editCategoryAutocomplete.showDropDown() }
                        }
                    } else {
                        binding.editCategoryAutocomplete.showDropDown()
                    }
                }
            }
            
            binding.editCategoryAutocomplete.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: Editable?) {
                    val text = s?.toString().orEmpty()
                    if (text.isEmpty()) {
                        // No category text -> no selected category ID
                        selectedCategoryId = null
                        adapter.filter.filter(null) {
                            binding.editCategoryAutocomplete.post { binding.editCategoryAutocomplete.showDropDown() }
                        }
                    } else {
                        // Try to match current text to a known category; if none matches, clear the ID
                        val matchedCategory = categories.firstOrNull { it.name == text }
                        selectedCategoryId = matchedCategory?.id
                    }
                }
            })
            
            // Re-apply selection if post was already loaded
            viewModel.post.value?.let { post ->
                val cat = categories.firstOrNull { it.id == post.categoryId }
                if (cat != null) {
                    binding.editCategoryAutocomplete.setText(cat.name, false)
                    selectedCategoryId = cat.id
                }
            }
        }

        viewModel.allCities.observe(viewLifecycleOwner) { cities ->
            cityList = cities
            if (cities.isNotEmpty()) {
                val cityNames = cities.map { it.second }
                val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, cityNames)
                binding.editCityAutocomplete.setAdapter(adapter)

                binding.editCityAutocomplete.setOnItemClickListener { _, _, position, _ ->
                    val selectedName = binding.editCityAutocomplete.adapter.getItem(position) as String
                    selectedCityId = cities.firstOrNull { it.second == selectedName }?.first
                }

                binding.editCityAutocomplete.setOnClickListener {
                    if (binding.editCityAutocomplete.text.isEmpty()) {
                        adapter.filter.filter(null) {
                            binding.editCityAutocomplete.post { binding.editCityAutocomplete.showDropDown() }
                        }
                    } else {
                        binding.editCityAutocomplete.showDropDown()
                    }
                }
                binding.editCityAutocomplete.setOnFocusChangeListener { _, hasFocus -> 
                    if (hasFocus) {
                        if (binding.editCityAutocomplete.text.isEmpty()) {
                            adapter.filter.filter(null) {
                                binding.editCityAutocomplete.post { binding.editCityAutocomplete.showDropDown() }
                            }
                        } else {
                            binding.editCityAutocomplete.showDropDown()
                        }
                    }
                }
                
                binding.editCityAutocomplete.addTextChangedListener(object : TextWatcher {
                    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                    override fun afterTextChanged(s: Editable?) {
                        if (s.isNullOrEmpty()) {
                            // When text is cleared, also clear the selected city id and reset the dropdown
                            selectedCityId = null
                            adapter.filter.filter(null) {
                                binding.editCityAutocomplete.post { binding.editCityAutocomplete.showDropDown() }
                            }
                        } else {
                            // When text is non-empty, ensure selectedCityId matches the current text (or clear it)
                            val currentText = s.toString()
                            val matchedCity = cities.firstOrNull { it.second == currentText }
                            selectedCityId = matchedCity?.first
                        }
                    }
                })

                // Re-apply selection if post was already loaded
                viewModel.post.value?.let { post ->
                    val city = cities.firstOrNull { it.first == post.cityId }
                    if (city != null) {
                        binding.editCityAutocomplete.setText(city.second, false)
                        selectedCityId = city.first
                    }
                }
            }
        }

        viewModel.post.observe(viewLifecycleOwner) { post ->
            post?.let {
                binding.editTitleEditText.setText(it.title)
                binding.editDescriptionEditText.setText(it.description)
                
                val cat = categoryList.firstOrNull { c -> c.id == it.categoryId }
                if (cat != null) {
                    binding.editCategoryAutocomplete.setText(cat.name, false)
                    selectedCategoryId = cat.id
                }

                val city = cityList.firstOrNull { c -> c.first == it.cityId }
                if (city != null) {
                    binding.editCityAutocomplete.setText(city.second, false)
                    selectedCityId = city.first
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
