package com.example.colman_android_final_assigment.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.*
import com.example.colman_android_final_assigment.base.Resource
import com.example.colman_android_final_assigment.model.User
import com.example.colman_android_final_assigment.repository.AuthRepository
import com.example.colman_android_final_assigment.repository.PostRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

class ProfileViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = AuthRepository(application)
    private val postRepository = PostRepository(application)
    private val auth = FirebaseAuth.getInstance()

    val userDetails: LiveData<Resource<User>>? = auth.currentUser?.uid?.let { uid ->
        repository.getUserDetails(uid)
    }

    val postedCount: LiveData<Int> = auth.currentUser?.uid?.let { uid ->
        postRepository.getPostCountByUser(uid)
    } ?: MutableLiveData(0)

    val givenCount: LiveData<Int> = auth.currentUser?.uid?.let { uid ->
        postRepository.getGivenCountByUser(uid)
    } ?: MutableLiveData(0)

    private val _updateState = MutableLiveData<Resource<Unit>>()
    val updateState: LiveData<Resource<Unit>> = _updateState

    fun updateProfile(name: String, phone: String, imageUri: Uri?) {
        _updateState.value = Resource.Loading
        viewModelScope.launch {
            _updateState.value = repository.updateProfile(name, phone, imageUri)
        }
    }

    fun logout() {
        viewModelScope.launch {
            repository.logout()
        }
    }
}
