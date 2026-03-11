package com.example.colman_android_final_assigment.repository

import android.content.Context
import androidx.lifecycle.LiveData
import com.example.colman_android_final_assigment.base.Resource
import com.example.colman_android_final_assigment.database.AppLocalDb
import com.example.colman_android_final_assigment.model.Post
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class PostRepository(context: Context) {

    private val firestore get() = FirebaseFirestore.getInstance()
    private val postDao = AppLocalDb.getDatabase(context).postDao()

    /** LiveData observed by the UI - sourced from local Room cache */
    fun getAllPostsLiveData(): LiveData<List<Post>> = postDao.getAllPosts()

    /**
     * Fetch all posts from Firestore and refresh the local Room cache.
     * Call this on swipe-to-refresh or when the feed loads.
     */
    suspend fun refreshPosts(): Resource<Unit> {
        return try {
            val snapshot = firestore.collection("posts").get().await()
            val posts = snapshot.documents.mapNotNull { doc ->
                val userId = when (val ref = doc.get("userId")) {
                    is com.google.firebase.firestore.DocumentReference -> ref.id
                    is String -> ref
                    else -> ""
                }
                Post(
                    id = doc.id,
                    title = doc.getString("title") ?: "",
                    description = doc.getString("description") ?: "",
                    category = doc.getString("category") ?: "",
                    cityId = (doc.getLong("cityId") ?: 0L).toInt(),
                    imageUrl = doc.getString("imageUrl") ?: "",
                    isTaken = doc.getBoolean("isTaken") ?: false,
                    userId = userId
                )
            }
            postDao.clearAll()
            postDao.insertAll(posts)
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Failed to refresh posts")
        }
    }
}

