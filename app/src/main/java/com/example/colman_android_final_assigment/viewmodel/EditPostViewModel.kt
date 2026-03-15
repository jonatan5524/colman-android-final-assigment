package com.example.colman_android_final_assigment.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.*
import com.example.colman_android_final_assigment.base.Resource
import com.example.colman_android_final_assigment.model.Post
import com.example.colman_android_final_assigment.repository.PostRepository
import com.example.colman_android_final_assigment.data.remote.CityApiService
import com.example.colman_android_final_assigment.model.Category
import com.example.colman_android_final_assigment.repository.CategoryRepository
import kotlinx.coroutines.launch

class EditPostViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = PostRepository(application)
    private val categoryRepository = CategoryRepository(application)
    private val _postId = MutableLiveData<String>()

    val categories: LiveData<List<Category>> = categoryRepository.getAllCategories()

    private val _allCities = MutableLiveData<List<Pair<Int, String>>>(emptyList())
    val allCities: LiveData<List<Pair<Int, String>>> = _allCities

    init {
        viewModelScope.launch {
            _allCities.value = CityApiService.getAllCities()
            categoryRepository.refreshCategories()
        }
    }

    val post: LiveData<Post?> = _postId.switchMap { id ->
        repository.getPostById(id)
    }

    private val _updateState = MutableLiveData<Resource<Unit>>()
    val updateState: LiveData<Resource<Unit>> = _updateState

    fun setPostId(id: String) {
        if (_postId.value == id) return
        _postId.value = id
        viewModelScope.launch {
            repository.refreshPost(id)
        }
    }

    fun savePost(
        title: String,
        description: String,
        categoryId: String,
        cityId: Int,
        imageUri: Uri?
    ) {
        val id = _postId.value ?: return
        _updateState.value = Resource.Loading
        viewModelScope.launch {
            _updateState.value = repository.updatePost(
                postId = id,
                title = title,
                description = description,
                categoryId = categoryId,
                cityId = cityId,
                imageUri = imageUri
            )
        }
    }
}
