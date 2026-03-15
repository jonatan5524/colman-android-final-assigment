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
import com.example.colman_android_final_assigment.R
import com.example.colman_android_final_assigment.base.Resource
import com.example.colman_android_final_assigment.databinding.FragmentNewPostBinding
import com.example.colman_android_final_assigment.model.Category
import com.example.colman_android_final_assigment.viewmodel.NewPostViewModel

class NewPostFragment : Fragment() {

    private var _binding: FragmentNewPostBinding? = null
    private val binding get() = _binding!!
    private val viewModel: NewPostViewModel by viewModels()
    private var imageUri: Uri? = null

    /** Currently loaded city list. Each entry is (cityId, cityName). */
    private var cityList: List<Pair<Int, String>> = emptyList()

    /** Currently loaded category list. */
    private var categoryList: List<Category> = emptyList()

    /** The category ID selected by the user via autocomplete. */
    private var selectedCategoryId: String? = null

    /** The city ID selected by the user via autocomplete. */
    private var selectedCityId: Int? = null

    private val pickImage = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            imageUri = it
            binding.postImage.setImageURI(it)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNewPostBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupImagePicker()
        observeViewModel()

        binding.cancelPostButton.setOnClickListener {
            findNavController().popBackStack()
        }

        binding.savePostButton.setOnClickListener {
            validateAndSave()
        }
    }

    /* ------------------------------------------------------------------ */
    /*  Image picker                                                       */
    /* ------------------------------------------------------------------ */

    private fun setupImagePicker() {
        binding.uploadImageButton.setOnClickListener {
            pickImage.launch("image/*")
        }
    }

    /* ------------------------------------------------------------------ */
    /*  Observers                                                          */
    /* ------------------------------------------------------------------ */

    private fun observeViewModel() {
        // Categories
        viewModel.categories.observe(viewLifecycleOwner) { categories ->
            categoryList = categories
            val names = categories.map { it.name }
            val adapter = ArrayAdapter(
                requireContext(),
                android.R.layout.simple_dropdown_item_1line,
                names
            )
            binding.categoryAutocomplete.setAdapter(adapter)

            binding.categoryAutocomplete.setOnItemClickListener { _, _, position, _ ->
                val selectedName = binding.categoryAutocomplete.adapter.getItem(position) as String
                selectedCategoryId = categories.firstOrNull { it.name == selectedName }?.id
            }

            binding.categoryAutocomplete.setOnClickListener {
                if (binding.categoryAutocomplete.text.isEmpty()) {
                    adapter.filter.filter(null) {
                        binding.categoryAutocomplete.post { binding.categoryAutocomplete.showDropDown() }
                    }
                } else {
                    binding.categoryAutocomplete.showDropDown()
                }
            }
            binding.categoryAutocomplete.setOnFocusChangeListener { _, hasFocus ->
                if (hasFocus) {
                    if (binding.categoryAutocomplete.text.isEmpty()) {
                        adapter.filter.filter(null) {
                            binding.categoryAutocomplete.post { binding.categoryAutocomplete.showDropDown() }
                        }
                    } else {
                        binding.categoryAutocomplete.showDropDown()
                    }
                }
            }
            
            binding.categoryAutocomplete.addTextChangedListener(object : android.text.TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: android.text.Editable?) {
                    if (s.isNullOrEmpty()) {
                        selectedCategoryId = null
                        adapter.filter.filter(null) {
                            binding.categoryAutocomplete.post { binding.categoryAutocomplete.showDropDown() }
                        }
                    }
                }
            })
        }

        // Cities loading state
        viewModel.citiesLoading.observe(viewLifecycleOwner) { loading ->
            binding.cityLoadingSpinner.visibility = if (loading) View.VISIBLE else View.GONE
            binding.cityAutocomplete.isEnabled = !loading
        }

        // Cities list
        viewModel.allCities.observe(viewLifecycleOwner) { cities ->
            cityList = cities
            if (cities.isNotEmpty()) {
                setupCityAutocomplete(cities)
            }
        }

        // Create result
        viewModel.createState.observe(viewLifecycleOwner) { resource ->
            binding.saveProgressBar.visibility = if (resource is Resource.Loading) View.VISIBLE else View.GONE
            binding.savePostButton.isEnabled = resource !is Resource.Loading

            when (resource) {
                is Resource.Success -> {
                    Toast.makeText(context, "Post created successfully!", Toast.LENGTH_SHORT).show()
                    findNavController().navigate(R.id.action_newPostFragment_to_feedFragment)
                }
                is Resource.Error -> {
                    Toast.makeText(context, resource.message, Toast.LENGTH_SHORT).show()
                }
                else -> {}
            }
        }
    }

    /* ------------------------------------------------------------------ */
    /*  City autocomplete                                                  */
    /* ------------------------------------------------------------------ */

    private fun setupCityAutocomplete(cities: List<Pair<Int, String>>) {
        val cityNames = cities.map { it.second }
        val adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_dropdown_item_1line,
            cityNames
        )
        binding.cityAutocomplete.setAdapter(adapter)

        binding.cityAutocomplete.setOnItemClickListener { _, _, position, _ ->
            // The position corresponds to the filtered adapter, so look up by name
            val selectedName = binding.cityAutocomplete.adapter.getItem(position) as String
            selectedCityId = cities.firstOrNull { it.second == selectedName }?.first
        }

        binding.cityAutocomplete.setOnClickListener {
            if (binding.cityAutocomplete.text.isEmpty()) {
                adapter.filter.filter(null) {
                    binding.cityAutocomplete.post { binding.cityAutocomplete.showDropDown() }
                }
            } else {
                binding.cityAutocomplete.showDropDown()
            }
        }
        binding.cityAutocomplete.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                if (binding.cityAutocomplete.text.isEmpty()) {
                    adapter.filter.filter(null) {
                        binding.cityAutocomplete.post { binding.cityAutocomplete.showDropDown() }
                    }
                } else {
                    binding.cityAutocomplete.showDropDown()
                }
            }
        }
        
        binding.cityAutocomplete.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) {
                if (s.isNullOrEmpty()) {
                    selectedCityId = null
                    adapter.filter.filter(null) {
                        binding.cityAutocomplete.post { binding.cityAutocomplete.showDropDown() }
                    }
                }
            }
        })
    }

    /* ------------------------------------------------------------------ */
    /*  Validation & Save                                                  */
    /* ------------------------------------------------------------------ */

    private fun validateAndSave() {
        val title = binding.titleEditText.text.toString().trim()
        val description = binding.descriptionEditText.text.toString().trim()

        var isValid = true

        if (title.isEmpty()) {
            binding.titleLayout.error = getString(R.string.error_empty_title)
            isValid = false
        } else {
            binding.titleLayout.error = null
        }

        if (description.isEmpty()) {
            binding.descriptionLayout.error = getString(R.string.error_empty_description)
            isValid = false
        } else {
            binding.descriptionLayout.error = null
        }

        // Category validation
        val categoryId = selectedCategoryId
        if (categoryId == null) {
            binding.categoryInputLayout.error = getString(R.string.error_empty_category)
            isValid = false
        } else {
            binding.categoryInputLayout.error = null
        }

        // City validation
        val cityId = selectedCityId
        if (cityId == null) {
            binding.cityInputLayout.error = getString(R.string.error_empty_city)
            isValid = false
        } else {
            binding.cityInputLayout.error = null
        }

        if (isValid) {
            viewModel.createPost(
                title = title,
                description = description,
                categoryId = categoryId!!,
                cityId = cityId!!,
                imageUri = imageUri
            )
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
