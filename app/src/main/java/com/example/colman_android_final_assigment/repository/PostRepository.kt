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

    /** LiveData for current user's posts */
    fun getPostsByUser(userId: String): LiveData<List<Post>> = postDao.getPostsByUser(userId)

    /**
     * Fetch all posts from Firestore and refresh the local Room cache.
     */
    suspend fun refreshPosts(): Resource<Unit> {
        return try {
            val snapshot = firestore.collection("posts").get().await()
            val posts = mapSnapshotToPosts(snapshot)
            postDao.clearAll()
            postDao.insertAll(posts)
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Failed to refresh posts")
        }
    }

    /**
     * Fetch only the user's posts from Firestore and update the local Room cache.
     */
    suspend fun refreshUserPosts(userId: String): Resource<Unit> {
        return try {

            var snapshot = firestore.collection("posts")
                .whereEqualTo("userId", userId)
                .get()
                .await()
            
            if (snapshot.isEmpty) {
                val userRef = firestore.collection("users").document(userId)
                snapshot = firestore.collection("posts")
                    .whereEqualTo("userId", userRef)
                    .get()
                    .await()
            }

            val posts = mapSnapshotToPosts(snapshot)
            // Insert or update user's posts in the cache
            postDao.insertAll(posts)
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Failed to refresh your posts")
        }
    }

    private fun mapSnapshotToPosts(snapshot: com.google.firebase.firestore.QuerySnapshot): List<Post> {
        return snapshot.documents.mapNotNull { doc ->
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
    }
}

