package com.example.colman_android_final_assigment.modules

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.example.colman_android_final_assigment.R
import com.example.colman_android_final_assigment.base.Resource
import com.example.colman_android_final_assigment.data.remote.CityApiService
import com.example.colman_android_final_assigment.databinding.FragmentFeedBinding
import com.example.colman_android_final_assigment.viewmodel.FeedViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class FeedFragment : Fragment() {

    private var _binding: FragmentFeedBinding? = null
    private val binding get() = _binding!!

    private val viewModel: FeedViewModel by viewModels()
    private lateinit var adapter: PostsAdapter

    /** Cached city-id → city-name map for the filter dialog */
    private var cityIdToNameMap: Map<Int, String> = emptyMap()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFeedBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupSearchBar()
        setupFilterButton()
        observeViewModel()

        binding.swipeRefreshLayout.setOnRefreshListener {
            viewModel.refreshPosts()
        }
    }

    /* ------------------------------------------------------------------ */
    /*  RecyclerView                                                       */
    /* ------------------------------------------------------------------ */

    private fun setupRecyclerView() {
        adapter = PostsAdapter()
        binding.feedRecyclerView.adapter = adapter
    }

    /* ------------------------------------------------------------------ */
    /*  Search bar (free-text, 400 ms debounce handled in VM)              */
    /* ------------------------------------------------------------------ */

    private fun setupSearchBar() {
        // Restore persisted query so the EditText stays in sync after config-change
        binding.searchEditText.setText(viewModel.searchQuery.value ?: "")
        binding.searchEditText.setSelection(binding.searchEditText.text?.length ?: 0)

        binding.searchEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                viewModel.setSearchQuery(s?.toString() ?: "")
            }
        })
    }

    /* ------------------------------------------------------------------ */
    /*  Filter button → Material dialog with Category + City spinners      */
    /* ------------------------------------------------------------------ */

    private fun setupFilterButton() {
        binding.filterButton.setOnClickListener {
            showFilterDialog()
        }
    }

    private fun showFilterDialog() {
        val cityIds = viewModel.availableCityIds.value ?: emptyList()

        // Pre-fetch city names, then open the dialog
        binding.feedProgressBar.visibility = View.VISIBLE
        viewLifecycleOwner.lifecycleScope.launch {
            withContext(Dispatchers.IO) { CityApiService.prefetchCities(cityIds) }
            cityIdToNameMap = cityIds.associateWith { CityApiService.getCityNameById(it) }
            binding.feedProgressBar.visibility = View.GONE
            buildFilterDialog(cityIds)
        }
    }

    private fun buildFilterDialog(cityIds: List<Int>) {
        val context = requireContext()

        /* --- Category spinner data --- */
        val categories = listOf(getString(R.string.filter_all_categories)) +
                (viewModel.availableCategories.value ?: emptyList())
        val currentCategory = viewModel.selectedCategory.value
        val categoryIndex = if (currentCategory == null) 0
        else categories.indexOf(currentCategory).coerceAtLeast(0)

        /* --- City spinner data --- */
        val cityNames = listOf(getString(R.string.filter_all_cities)) +
                cityIds.map { cityIdToNameMap[it] ?: getString(R.string.filter_city_fallback, it) }
        val cityValues = listOf(null) + cityIds
        val currentCityId = viewModel.selectedCityId.value
        val cityIndex = cityValues.indexOf(currentCityId).coerceAtLeast(0)

        /* --- Build dialog layout programmatically --- */
        val padding = (24 * resources.displayMetrics.density).toInt()

        val container = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(padding, padding / 2, padding, 0)
        }

        // Category label + spinner
        val categoryLabel = TextView(context).apply {
            text = getString(R.string.filter_label_category)
            setTextColor(ContextCompat.getColor(context, R.color.text_dark))
            textSize = 14f
        }
        val categorySpinner = Spinner(context).apply {
            adapter = ArrayAdapter(context, android.R.layout.simple_spinner_dropdown_item, categories)
            setSelection(categoryIndex)
        }
        container.addView(categoryLabel)
        container.addView(categorySpinner)

        // City label + spinner
        val cityLabel = TextView(context).apply {
            text = getString(R.string.filter_label_city)
            setTextColor(ContextCompat.getColor(context, R.color.text_dark))
            textSize = 14f
        }
        val citySpinner = Spinner(context).apply {
            adapter = ArrayAdapter(context, android.R.layout.simple_spinner_dropdown_item, cityNames)
            setSelection(cityIndex)
        }
        container.addView(cityLabel)
        container.addView(citySpinner)

        /* --- Show dialog --- */
        MaterialAlertDialogBuilder(context)
            .setTitle(getString(R.string.filter_dialog_title))
            .setView(container)
            .setPositiveButton(getString(R.string.filter_action_apply)) { dialog, _ ->
                val selectedCatPos = categorySpinner.selectedItemPosition
                val selectedCityPos = citySpinner.selectedItemPosition

                val cat = if (selectedCatPos == 0) null else categories[selectedCatPos]
                val city = cityValues[selectedCityPos]

                viewModel.setCategory(cat)
                viewModel.setCity(city)
                dialog.dismiss()
            }
            .setNeutralButton(getString(R.string.filter_action_reset)) { dialog, _ ->
                viewModel.resetFilters()
                binding.searchEditText.text?.clear()
                dialog.dismiss()
            }
            .setNegativeButton(getString(R.string.filter_action_cancel)) { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    /* ------------------------------------------------------------------ */
    /*  Observers                                                          */
    /* ------------------------------------------------------------------ */

    private fun observeViewModel() {
        // Observe filtered posts (replaces the old allPosts observer)
        viewModel.filteredPosts.observe(viewLifecycleOwner) { posts ->
            adapter.submitList(posts)
        }

        // Keep categories & city IDs alive so .value is populated for the filter dialog
        viewModel.availableCategories.observe(viewLifecycleOwner) { /* no-op */ }
        viewModel.availableCityIds.observe(viewLifecycleOwner) { /* no-op */ }

        viewModel.refreshState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is Resource.Loading -> {
                    binding.feedProgressBar.visibility = View.VISIBLE
                }
                is Resource.Success -> {
                    binding.feedProgressBar.visibility = View.GONE
                    binding.swipeRefreshLayout.isRefreshing = false
                }
                is Resource.Error -> {
                    binding.feedProgressBar.visibility = View.GONE
                    binding.swipeRefreshLayout.isRefreshing = false
                    Snackbar.make(binding.root, state.message, Snackbar.LENGTH_LONG).show()
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
