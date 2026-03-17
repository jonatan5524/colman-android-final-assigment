package com.example.colman_android_final_assigment.modules

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
import androidx.navigation.fragment.findNavController
import com.example.colman_android_final_assigment.model.Post
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
        adapter = PostsAdapter(onPostClick = { post: Post ->
            val action = FeedFragmentDirections.actionFeedFragmentToPostDetailsFragment(post.id)
            findNavController().navigate(action)
        })
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
        buildFilterDialog(cityIds)
    }

    private fun buildFilterDialog(cityIds: List<Int>) {
        val context = requireContext()

        /* --- Category data --- */
        val categories = viewModel.availableCategories.value ?: emptyList()
        val categoryIds = categories.map { it.first }
        val categoryNames = categories.map { it.second }
        val categoryNameById = categories.associate { it.first to it.second }
        val selectedCategoryIds = (viewModel.selectedCategoryIds.value ?: emptyList()).toMutableSet()

        /* --- City data --- */
        val selectedCityIds = (viewModel.selectedCityIds.value ?: emptyList()).toMutableSet()

        /* --- Build dialog layout programmatically --- */
        val padding = (24 * resources.displayMetrics.density).toInt()
        val dropdownBg = ContextCompat.getDrawable(context, R.drawable.bg_input)

        val container = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(padding, padding / 2, padding, 0)
        }

        // Helper to format summary text for a dropdown
        fun summaryText(selected: Collection<String>, allLabel: String): String {
            return when {
                selected.isEmpty() -> allLabel
                selected.size == 1 -> selected.first()
                else -> "${selected.size} selected"
            }
        }

        // ── Category label + dropdown trigger ──
        val selectedCategoryNames = selectedCategoryIds.mapNotNull { id -> categoryNameById[id] }
        val categoryLabel = TextView(context).apply {
            text = getString(R.string.filter_label_category)
            setTextColor(ContextCompat.getColor(context, R.color.text_dark))
            textSize = 14f
        }
        val categoryDropdown = TextView(context).apply {
            text = summaryText(selectedCategoryNames, getString(R.string.filter_all_categories))
            setTextColor(ContextCompat.getColor(context, R.color.text_dark))
            textSize = 14f
            background = dropdownBg?.constantState?.newDrawable()?.mutate()
            setPadding(padding / 2, padding / 3, padding / 2, padding / 3)
            setCompoundDrawablesRelativeWithIntrinsicBounds(0, 0, R.drawable.ic_arrow_drop_down, 0)
            compoundDrawablePadding = 8
        }
        categoryDropdown.setOnClickListener {
            val items = categoryNames.toTypedArray()
            val checked = BooleanArray(items.size) { categoryIds[it] in selectedCategoryIds }
            MaterialAlertDialogBuilder(context)
                .setTitle(getString(R.string.filter_label_category))
                .setMultiChoiceItems(items, checked) { _, which, isChecked ->
                    if (isChecked) selectedCategoryIds.add(categoryIds[which])
                    else selectedCategoryIds.remove(categoryIds[which])
                }
                .setPositiveButton(android.R.string.ok) { d, _ ->
                    val updatedNames = selectedCategoryIds.mapNotNull { id -> categoryNameById[id] }
                    categoryDropdown.text = summaryText(updatedNames, getString(R.string.filter_all_categories))
                    d.dismiss()
                }
                .show()
        }
        container.addView(categoryLabel)
        container.addView(categoryDropdown)

        // Spacer
        container.addView(View(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, (12 * resources.displayMetrics.density).toInt()
            )
        })

        // ── City label + dropdown trigger ──
        val cityLabel = TextView(context).apply {
            text = getString(R.string.filter_label_city)
            setTextColor(ContextCompat.getColor(context, R.color.text_dark))
            textSize = 14f
        }
        val selectedCityNames = selectedCityIds.mapNotNull { id ->
            cityIdToNameMap[id]
        }
        val cityDropdown = TextView(context).apply {
            text = summaryText(selectedCityNames, getString(R.string.filter_all_cities))
            setTextColor(ContextCompat.getColor(context, R.color.text_dark))
            textSize = 14f
            background = dropdownBg?.constantState?.newDrawable()?.mutate()
            setPadding(padding / 2, padding / 3, padding / 2, padding / 3)
            setCompoundDrawablesRelativeWithIntrinsicBounds(0, 0, R.drawable.ic_arrow_drop_down, 0)
            compoundDrawablePadding = 8
        }
        cityDropdown.setOnClickListener {
            // Recompute names from the current map (may have been populated since dialog was built)
            val currentCityNames = cityIds.map { cityIdToNameMap[it] ?: getString(R.string.filter_city_fallback, it) }
            val items = currentCityNames.toTypedArray()
            val checked = BooleanArray(items.size) { cityIds[it] in selectedCityIds }
            MaterialAlertDialogBuilder(context)
                .setTitle(getString(R.string.filter_label_city))
                .setMultiChoiceItems(items, checked) { _, which, isChecked ->
                    if (isChecked) selectedCityIds.add(cityIds[which])
                    else selectedCityIds.remove(cityIds[which])
                }
                .setPositiveButton(android.R.string.ok) { d, _ ->
                    val updatedNames = selectedCityIds.mapNotNull { id -> cityIdToNameMap[id] }
                    cityDropdown.text = summaryText(updatedNames, getString(R.string.filter_all_cities))
                    d.dismiss()
                }
                .show()
        }
        container.addView(cityLabel)
        container.addView(cityDropdown)

        /* --- Show main filter dialog --- */
        MaterialAlertDialogBuilder(context)
            .setTitle(getString(R.string.filter_dialog_title))
            .setView(container)
            .setPositiveButton(getString(R.string.filter_action_apply)) { dialog, _ ->
                viewModel.setCategories(selectedCategoryIds.toList())
                viewModel.setCityIds(selectedCityIds.toList())
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

        viewModel.categoryNameById.observe(viewLifecycleOwner) { categoryMap ->
            adapter.updateCategoryNameMap(categoryMap)
        }

        // Proactively prefetch city names whenever the city ID list changes
        viewModel.availableCityIds.observe(viewLifecycleOwner) { cityIds ->
            if (cityIds.isNullOrEmpty()) return@observe
            viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
                val limitedIds = cityIds.take(20)
                CityApiService.prefetchCities(limitedIds)
                val updatedMap = limitedIds.associateWith { CityApiService.getCityNameById(it) }
                withContext(Dispatchers.Main) {
                    cityIdToNameMap = cityIdToNameMap + updatedMap
                }
            }
        }

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
