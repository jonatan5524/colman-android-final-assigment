package com.example.colman_android_final_assigment.repository

import android.content.Context
import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.map
import com.example.colman_android_final_assigment.base.Resource
import com.example.colman_android_final_assigment.database.AppLocalDb
import com.example.colman_android_final_assigment.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await

class AuthRepository(context: Context) {
    private val auth get() = FirebaseAuth.getInstance()
    private val firestore get() = FirebaseFirestore.getInstance()
    private val storage get() = FirebaseStorage.getInstance()
    private val userDao = AppLocalDb.getDatabase(context).userDao()

    fun getCurrentUser(): User? {
        val firebaseUser = auth.currentUser
        return firebaseUser?.let {
            User(
                id = it.uid,
                name = it.displayName ?: "",
                email = it.email ?: "",
                phone = it.phoneNumber ?: ""
            )
        }
    }

    fun getCurrentUserId(): String? = auth.currentUser?.uid

    fun observeCachedUserById(userId: String): LiveData<User?> = userDao.getUserById(userId)

    suspend fun login(email: String, pass: String): Resource<Unit> {
        return try {
            auth.signInWithEmailAndPassword(email, pass).await()
            val uid = auth.currentUser?.uid
            if (uid != null) {
                refreshUserDetails(uid)
            }
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Login failed")
        }
    }

    suspend fun register(name: String, email: String, pass: String, phone: String, imageUri: Uri?): Resource<Unit> {
        return try {
            val authResult = auth.createUserWithEmailAndPassword(email, pass).await()
            val uid = authResult.user?.uid ?: throw Exception("User creation failed")

            var avatarUrl = ""
            if (imageUri != null) {
                val ref = storage.reference.child("avatars/$uid.jpg")
                ref.putFile(imageUri).await()
                avatarUrl = ref.downloadUrl.await().toString()
            }

            val user = User(id = uid, name = name, email = email, phone = phone, avatarUrl = avatarUrl)
            firestore.collection("users").document(uid).set(user).await()
            
            // Cache locally
            userDao.insertUser(user)

            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Registration failed")
        }
    }

    suspend fun updateProfile(name: String, phone: String, imageUri: Uri?): Resource<Unit> {
        return try {
            val uid = auth.currentUser?.uid ?: throw Exception("Not logged in")
            val updates = mutableMapOf<String, Any>(
                "name" to name,
                "phone" to phone
            )

            if (imageUri != null) {
                val ref = storage.reference.child("avatars/$uid.jpg")
                ref.putFile(imageUri).await()
                val avatarUrl = ref.downloadUrl.await().toString()
                updates["avatarUrl"] = avatarUrl
            }

            firestore.collection("users").document(uid).update(updates).await()
            
            // Sync local cache
            refreshUserDetails(uid)
            
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Update failed")
        }
    }

    suspend fun refreshUserDetails(uid: String) {
        try {
            val snapshot = firestore.collection("users").document(uid).get().await()
            val user = snapshot.toObject(User::class.java)
            if (user != null) {
                userDao.insertUser(user)
            }
        } catch (_: Exception) {
            Resource.Error(e.localizedMessage ?: "Refresh user details failed")
        }
    }

    fun getUserDetails(uid: String): LiveData<Resource<User>> {
        // Return local data as Resource
        return userDao.getUserById(uid).map { user ->
            if (user != null) Resource.Success(user) else Resource.Loading
        }
    }

    suspend fun logout() {
        auth.signOut()
        userDao.clearAll()
    }
}
