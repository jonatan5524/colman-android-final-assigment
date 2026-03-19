package com.example.colman_android_final_assigment.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.switchMap
import androidx.lifecycle.viewModelScope
import com.example.colman_android_final_assigment.model.Post
import com.example.colman_android_final_assigment.model.User
import com.example.colman_android_final_assigment.repository.AuthRepository
import com.example.colman_android_final_assigment.repository.CategoryRepository
import com.example.colman_android_final_assigment.repository.PostRepository
import kotlinx.coroutines.launch

class PostDetailsViewModel(application: Application) : AndroidViewModel(application) {
    private val postRepository = PostRepository(application)
    private val categoryRepository = CategoryRepository(application)
    private val authRepository = AuthRepository(application)

    private val postId = MutableLiveData<String>()
    private val ownerUserId = MutableLiveData<String>()

    val post: LiveData<Post?> = postId.switchMap { id ->
        postRepository.getPostById(id)
    }

    val ownerUser: LiveData<User?> = ownerUserId.switchMap { userId ->
        authRepository.observeCachedUserById(userId)
    }

    val currentUser: LiveData<User?> = authRepository.getCurrentUserId()?.let { uid ->
        authRepository.observeCachedUserById(uid)
    } ?: MutableLiveData(null)

    private val _categoryNameById = MediatorLiveData<Map<String, String>>(emptyMap())
    val categoryNameById: LiveData<Map<String, String>> = _categoryNameById

    init {
        _categoryNameById.addSource(categoryRepository.getAllCategories()) { categories ->
            _categoryNameById.value = categories.associate { category ->
                category.id to category.name
            }
        }

        authRepository.getCurrentUserId()?.let { uid ->
            viewModelScope.launch {
                authRepository.refreshUserDetails(uid)
            }
        }

        viewModelScope.launch {
            categoryRepository.refreshCategories()
        }
    }

    fun setPostId(id: String) {
        if (postId.value == id) return
        postId.value = id
        viewModelScope.launch {
            postRepository.refreshPost(id)
        }
    }

    fun setOwnerUserId(userId: String) {
        if (userId.isBlank() || ownerUserId.value == userId) return
        ownerUserId.value = userId
        viewModelScope.launch {
            authRepository.refreshUserDetails(userId)
        }
    }
}


