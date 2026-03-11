package com.example.colman_android_final_assigment.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.colman_android_final_assigment.base.Resource
import com.example.colman_android_final_assigment.model.Post
import com.example.colman_android_final_assigment.repository.PostRepository
import kotlinx.coroutines.launch

class FeedViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = PostRepository(application)

    /** All posts sourced from the local Room cache */
    val allPosts: LiveData<List<Post>> = repository.getAllPostsLiveData()

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
}

