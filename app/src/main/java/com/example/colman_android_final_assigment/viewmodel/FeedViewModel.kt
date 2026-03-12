package com.example.colman_android_final_assigment.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.switchMap
import androidx.lifecycle.viewModelScope
import com.example.colman_android_final_assigment.base.Resource
import com.example.colman_android_final_assigment.model.Post
import com.example.colman_android_final_assigment.repository.PostRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class FeedViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = PostRepository(application)

    /* ---------- Search & filter state ---------- */

    private val _searchQuery = MutableLiveData("")
    val searchQuery: LiveData<String> = _searchQuery

    private val _selectedCategories = MutableLiveData<List<String>>(emptyList())
    val selectedCategories: LiveData<List<String>> = _selectedCategories

    private val _selectedCityIds = MutableLiveData<List<Int>>(emptyList())
    val selectedCityIds: LiveData<List<Int>> = _selectedCityIds

    /** Internal debounced query – drives the actual Room lookup. */
    private val _debouncedQuery = MutableLiveData("")
    private var debounceJob: Job? = null

    /* ---------- Exposed LiveData ---------- */


    /** Filtered posts – observed by the adapter */
    val filteredPosts: LiveData<List<Post>> = _debouncedQuery.switchMap { query ->
        repository.getFilteredPostsMultiLiveData(
            query,
            _selectedCategories.value ?: emptyList(),
            _selectedCityIds.value ?: emptyList()
        )
    }

    /** Dynamic list of categories currently in cache */
    val availableCategories: LiveData<List<String>> = repository.getAllCategoriesLiveData()

    /** Dynamic list of city IDs currently in cache */
    val availableCityIds: LiveData<List<Int>> = repository.getAllCityIdsLiveData()

    /* ---------- Refresh ---------- */

    private val _refreshState = MutableLiveData<Resource<Unit>>()
    val refreshState: LiveData<Resource<Unit>> = _refreshState

    init {
        refreshPosts()
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

    /** Set categories filter (empty list = all categories). */
    fun setCategories(categories: List<String>) {
        _selectedCategories.value = categories
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
        _selectedCategories.value = emptyList()
        _selectedCityIds.value = emptyList()
        debounceJob?.cancel()
        debounceJob = null
        _debouncedQuery.value = ""
    }

    /** Re-trigger the switchMap so category/city changes take effect. */
    private fun reapplyFilter() {
        _debouncedQuery.value = _debouncedQuery.value ?: ""
    }
}

