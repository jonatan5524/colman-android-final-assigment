package com.example.colman_android_final_assigment.viewmodel

import android.app.Application
import androidx.lifecycle.*
import com.example.colman_android_final_assigment.base.Resource
import com.example.colman_android_final_assigment.model.Post
import com.example.colman_android_final_assigment.repository.PostRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

class MyPostsViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = PostRepository(application)
    private val userId: String = FirebaseAuth.getInstance().currentUser?.uid ?: ""

    // Observable LiveData from Room
    val myPosts: LiveData<List<Post>> = if (userId.isNotEmpty()) {
        repository.getPostsByUser(userId)
    } else {
        MutableLiveData(emptyList())
    }

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

    init {
        refreshPosts()
    }

    fun refreshPosts() {
        if (userId.isEmpty()) return
        
        viewModelScope.launch {
            _isLoading.value = true
            val result = repository.refreshUserPosts(userId)
            if (result is Resource.Error) {
                _errorMessage.value = result.message
            } else {
                _errorMessage.value = null
            }
            _isLoading.value = false
        }
    }
}
