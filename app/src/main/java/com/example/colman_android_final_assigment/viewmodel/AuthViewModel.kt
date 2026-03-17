package com.example.colman_android_final_assigment.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.colman_android_final_assigment.base.Resource
import com.example.colman_android_final_assigment.repository.AuthRepository
import kotlinx.coroutines.launch

class AuthViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = AuthRepository(application)

    private val _authState = MutableLiveData<Resource<Unit>>()
    val authState: LiveData<Resource<Unit>> = _authState

    fun login(email: String, pass: String) {
        _authState.value = Resource.Loading
        viewModelScope.launch {
            _authState.value = repository.login(email, pass)
        }
    }

    fun register(name: String, email: String, pass: String, phone: String, imageUri: Uri?) {
        _authState.value = Resource.Loading
        viewModelScope.launch {
            _authState.value = repository.register(name, email, pass, phone, imageUri)
        }
    }

    fun isUserLoggedIn(): Boolean {
        return repository.getCurrentUser() != null
    }
}
