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

    private val _selectedCategory = MutableLiveData<String?>(null)
    val selectedCategory: LiveData<String?> = _selectedCategory

    private val _selectedCityId = MutableLiveData<Int?>(null)
    val selectedCityId: LiveData<Int?> = _selectedCityId

    /** Internal debounced query – drives the actual Room lookup. */
    private val _debouncedQuery = MutableLiveData("")
    private var debounceJob: Job? = null

    /* ---------- Exposed LiveData ---------- */


    /** Filtered posts – observed by the adapter */
    val filteredPosts: LiveData<List<Post>> = _debouncedQuery.switchMap { query ->
        repository.getFilteredPostsLiveData(query, _selectedCategory.value, _selectedCityId.value)
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

    /** Set category filter (null = all categories). */
    fun setCategory(category: String?) {
        _selectedCategory.value = category
        reapplyFilter()
    }

    /** Set city filter by ID (null = all cities). */
    fun setCity(cityId: Int?) {
        _selectedCityId.value = cityId
        reapplyFilter()
    }

    /** Clear all filters and the search bar text. */
    fun resetFilters() {
        _searchQuery.value = ""
        _selectedCategory.value = null
        _selectedCityId.value = null
        _debouncedQuery.value = ""
    }

    /** Re-trigger the switchMap so category/city changes take effect. */
    private fun reapplyFilter() {
        _debouncedQuery.value = _debouncedQuery.value ?: ""
    }
}

