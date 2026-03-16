package com.example.colman_android_final_assigment.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.switchMap
import androidx.lifecycle.viewModelScope
import com.example.colman_android_final_assigment.base.Resource
import com.example.colman_android_final_assigment.model.Post
import com.example.colman_android_final_assigment.repository.CategoryRepository
import com.example.colman_android_final_assigment.repository.PostRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class FeedViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = PostRepository(application)
    private val categoryRepository = CategoryRepository(application)

    /* ---------- Search & filter state ---------- */

    private val _searchQuery = MutableLiveData("")
    val searchQuery: LiveData<String> = _searchQuery

    private val _selectedCategoryIds = MutableLiveData<List<String>>(emptyList())
    val selectedCategoryIds: LiveData<List<String>> = _selectedCategoryIds

    private val _selectedCityIds = MutableLiveData<List<Int>>(emptyList())
    val selectedCityIds: LiveData<List<Int>> = _selectedCityIds

    /** Internal debounced query – drives the actual Room lookup. */
    private val _debouncedQuery = MutableLiveData("")
    private var debounceJob: Job? = null

    /* ---------- Exposed LiveData ---------- */

    // UI category options as (categoryId, categoryName)
    private val _availableCategories = MutableLiveData<List<Pair<String, String>>>(emptyList())
    val availableCategories: LiveData<List<Pair<String, String>>> = _availableCategories

    // Fast lookup for adapters (categoryId -> categoryName)
    private val _categoryNameById = MutableLiveData<Map<String, String>>(emptyMap())
    val categoryNameById: LiveData<Map<String, String>> = _categoryNameById

    private var cachedPostCategoryIds: List<String> = emptyList()
    private var cachedCategoryNameById: Map<String, String> = emptyMap()

    private val postCategoryIdsSource = repository.getAllCategoryIdsLiveData()
    private val categoriesSource = categoryRepository.getAllCategories()

    private val postCategoryIdsObserver = Observer<List<String>> { ids ->
        cachedPostCategoryIds = ids
        rebuildCategoryState()
    }

    private val categoriesObserver = Observer<List<com.example.colman_android_final_assigment.model.Category>> { categories ->
        cachedCategoryNameById = categories
            .filter { it.id.isNotBlank() }
            .associate { it.id to it.name }
        rebuildCategoryState()
    }

    /** Filtered posts – observed by the adapter */
    val filteredPosts: LiveData<List<Post>> = _debouncedQuery.switchMap { query ->
        repository.getFilteredPostsMultiLiveData(
            query,
            _selectedCategoryIds.value ?: emptyList(),
            _selectedCityIds.value ?: emptyList()
        )
    }

    /** Dynamic list of city IDs currently in cache */
    val availableCityIds: LiveData<List<Int>> = repository.getAllCityIdsLiveData()

    /* ---------- Refresh ---------- */

    private val _refreshState = MutableLiveData<Resource<Unit>>()
    val refreshState: LiveData<Resource<Unit>> = _refreshState

    init {
        postCategoryIdsSource.observeForever(postCategoryIdsObserver)
        categoriesSource.observeForever(categoriesObserver)

        refreshCategories()
        refreshPosts()
    }

    override fun onCleared() {
        postCategoryIdsSource.removeObserver(postCategoryIdsObserver)
        categoriesSource.removeObserver(categoriesObserver)
        super.onCleared()
    }

    fun refreshPosts() {
        viewModelScope.launch {
            _refreshState.value = Resource.Loading
            _refreshState.value = repository.refreshPosts()
        }
    }

    /* ---------- Filter setters ---------- */

    /** Called on every keystroke – applies 400 ms debounce. */
    fun setSearchQuery(query: String) {
        _searchQuery.value = query
        debounceJob?.cancel()
        debounceJob = viewModelScope.launch {
            delay(400L)
            _debouncedQuery.value = query
        }
    }

    /** Set category IDs filter (empty list = all categories). */
    fun setCategories(categoryIds: List<String>) {
        _selectedCategoryIds.value = categoryIds.distinct()
        reapplyFilter()
    }

    /** Set city IDs filter (empty list = all cities). */
    fun setCityIds(cityIds: List<Int>) {
        _selectedCityIds.value = cityIds
        reapplyFilter()
    }

    /** Clear all filters and the search bar text. */
    fun resetFilters() {
        _searchQuery.value = ""
        _selectedCategoryIds.value = emptyList()
        _selectedCityIds.value = emptyList()
        debounceJob?.cancel()
        debounceJob = null
        _debouncedQuery.value = ""
    }

    /** Re-trigger the switchMap so category/city changes take effect. */
    private fun reapplyFilter() {
        _debouncedQuery.value = _debouncedQuery.value ?: ""
    }

    private fun refreshCategories() {
        viewModelScope.launch {
            categoryRepository.refreshCategories()
        }
    }

    private fun rebuildCategoryState() {
        val idToName = cachedPostCategoryIds.associateWith { categoryId ->
            cachedCategoryNameById[categoryId].orEmpty().ifBlank { categoryId }
        }

        _categoryNameById.value = idToName
        _availableCategories.value = cachedPostCategoryIds.map { id ->
            id to (idToName[id] ?: id)
        }

        val validSelected = (_selectedCategoryIds.value ?: emptyList())
            .filter { idToName.containsKey(it) }
        if (validSelected != _selectedCategoryIds.value) {
            _selectedCategoryIds.value = validSelected
            reapplyFilter()
        }
    }
}

