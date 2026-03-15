package com.example.colman_android_final_assigment.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.*
import com.example.colman_android_final_assigment.base.Resource
import com.example.colman_android_final_assigment.data.remote.CityApiService
import com.example.colman_android_final_assigment.model.Category
import com.example.colman_android_final_assigment.repository.CategoryRepository
import com.example.colman_android_final_assigment.repository.PostRepository
import kotlinx.coroutines.launch

class NewPostViewModel(application: Application) : AndroidViewModel(application) {

    private val postRepository = PostRepository(application)
    private val categoryRepository = CategoryRepository(application)

    /** Categories from Room cache */
    val categories: LiveData<List<Category>> = categoryRepository.getAllCategories()

    /** All cities fetched in bulk from the API */
    private val _allCities = MutableLiveData<List<Pair<Int, String>>>(emptyList())
    val allCities: LiveData<List<Pair<Int, String>>> = _allCities

    /** Whether cities are currently loading */
    private val _citiesLoading = MutableLiveData(true)
    val citiesLoading: LiveData<Boolean> = _citiesLoading

    /** Result of creating a post */
    private val _createState = MutableLiveData<Resource<Unit>>()
    val createState: LiveData<Resource<Unit>> = _createState

    init {
        loadCities()
        refreshCategories()
    }

    private fun loadCities() {
        viewModelScope.launch {
            _citiesLoading.value = true
            val cities = CityApiService.getAllCities()
            _allCities.value = cities
            _citiesLoading.value = false
        }
    }

    private fun refreshCategories() {
        viewModelScope.launch {
            categoryRepository.refreshCategories()
        }
    }

    fun createPost(
        title: String,
        description: String,
        categoryId: String,
        cityId: Int,
        whatsappNumber: String,
        imageUri: Uri?
    ) {
        _createState.value = Resource.Loading
        viewModelScope.launch {
            _createState.value = postRepository.createPost(
                title = title,
                description = description,
                categoryId = categoryId,
                cityId = cityId,
                whatsappNumber = whatsappNumber,
                imageUri = imageUri
            )
        }
    }
}
