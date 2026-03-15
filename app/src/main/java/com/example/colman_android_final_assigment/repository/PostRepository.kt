package com.example.colman_android_final_assigment.repository

import android.content.Context
import android.net.Uri
import androidx.lifecycle.LiveData
import com.example.colman_android_final_assigment.base.Resource
import com.example.colman_android_final_assigment.database.AppLocalDb
import com.example.colman_android_final_assigment.model.Post
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await

class PostRepository(context: Context) {

    private val firestore get() = FirebaseFirestore.getInstance()
    private val postDao = AppLocalDb.getDatabase(context).postDao()

    /** Filtered posts: free-text search + multi-select category/city filter */
    fun getFilteredPostsMultiLiveData(
        query: String,
        categoryIds: List<String>,
        cityIds: List<Int>
    ): LiveData<List<Post>> {
        return if (query.isBlank() && categoryIds.isEmpty() && cityIds.isEmpty()) {
            postDao.getAllPosts()
        } else {
            postDao.searchAndFilterMulti(
                query = query,
                categoryIds = categoryIds.ifEmpty { listOf("") },
                noCategories = if (categoryIds.isEmpty()) 1 else 0,
                cityIds = cityIds.ifEmpty { listOf(-1) },
                noCities = if (cityIds.isEmpty()) 1 else 0
            )
        }
    }

    /** All unique category IDs from the local cache */
    fun getAllCategoryIdsLiveData(): LiveData<List<String>> = postDao.getAllCategoryIds()

    /** All unique city IDs from the local cache */
    fun getAllCityIdsLiveData(): LiveData<List<Int>> = postDao.getAllCityIds()

    /** LiveData for current user's posts */
    fun getPostsByUser(userId: String): LiveData<List<Post>> = postDao.getPostsByUser(userId)

    /** LiveData for a single post */
    fun getPostById(postId: String): LiveData<Post?> = postDao.getPostById(postId)

    /** LiveData for total post count of a user */
    fun getPostCountByUser(userId: String): LiveData<Int> = postDao.getPostCountByUser(userId)

    /** LiveData for given post count of a user */
    fun getGivenCountByUser(userId: String): LiveData<Int> = postDao.getGivenCountByUser(userId)

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
            // Try querying by String first
            var snapshot = firestore.collection("posts")
                .whereEqualTo("userId", userId)
                .get()
                .await()
            
            // If empty, try querying by DocumentReference (common Firestore pattern)
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
            
            // REMOVE STALE DATA: Remove local posts for this user that are no longer in Firestore
            val currentPostIds = posts.map { it.id }
            if (currentPostIds.isNotEmpty()) {
                postDao.deleteUserPostsNotInList(userId, currentPostIds)
            } else {
                // Firestore returned no posts for this user; remove any stale local posts
                postDao.deletePostsByUserId(userId)
            }

            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Failed to refresh your posts")
        }
    }

    suspend fun refreshPost(postId: String): Resource<Unit> {
        return try {
            val doc = firestore.collection("posts").document(postId).get().await()
            val post = mapDocumentToPost(doc)
            if (post != null) {
                postDao.insertPost(post)
            }
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Failed to refresh post")
        }
    }

    /**
     * Create a new post: upload image → save to Firestore → cache in Room.
     */
    suspend fun createPost(
        title: String,
        description: String,
        categoryId: String,
        cityId: Int,
        whatsappNumber: String,
        imageUri: Uri?
    ): Resource<Unit> {
        return try {
            val userId = FirebaseAuth.getInstance().currentUser?.uid
                ?: throw Exception("Not logged in")
            val storage = FirebaseStorage.getInstance()

            // Generate a new document ID
            val docRef = firestore.collection("posts").document()
            val postId = docRef.id

            // Upload image if provided
            var imageUrl = ""
            if (imageUri != null) {
                val ref = storage.reference.child("posts/$postId.jpg")
                ref.putFile(imageUri).await()
                imageUrl = ref.downloadUrl.await().toString()
            }

            // Build the post data
            val postData = hashMapOf(
                "title" to title,
                "description" to description,
                "categoryId" to categoryId,
                "cityId" to cityId,
                "whatsappNumber" to whatsappNumber,
                "imageUrl" to imageUrl,
                "isTaken" to false,
                "userId" to userId
            )

            // Save to Firestore
            docRef.set(postData).await()

            // Cache locally in Room
            val post = Post(
                id = postId,
                title = title,
                description = description,
                categoryId = categoryId,
                cityId = cityId,
                imageUrl = imageUrl,
                isTaken = false,
                userId = userId
            )
            postDao.insertPost(post)

            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Failed to create post")
        }
    }

    /**
     * Delete a post from Firestore and the local Room cache.
     */
    suspend fun deletePost(post: Post): Resource<Unit> {
        return try {
            firestore.collection("posts").document(post.id).delete().await()
            postDao.deletePost(post)
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Failed to delete post")
        }
    }

    /**
     * Update the isTaken status of a post in Firestore and the local Room cache.
     */
    suspend fun updatePostStatus(post: Post, isTaken: Boolean): Resource<Unit> {
        return try {
            firestore.collection("posts").document(post.id).update("isTaken", isTaken).await()
            postDao.insertPost(post.copy(isTaken = isTaken))
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Failed to update status")
        }
    }

    suspend fun updatePost(
        postId: String,
        title: String,
        description: String,
        categoryId: String,
        cityId: Int,
        imageUri: Uri?
    ): Resource<Unit> {
        return try {
            val storage = FirebaseStorage.getInstance()
            val updates = mutableMapOf<String, Any>(
                "title" to title,
                "description" to description,
                "categoryId" to categoryId,
                "cityId" to cityId
            )

            if (imageUri != null) {
                val ref = storage.reference.child("posts/$postId.jpg")
                ref.putFile(imageUri).await()
                val imageUrl = ref.downloadUrl.await().toString()
                updates["imageUrl"] = imageUrl
            }

            firestore.collection("posts").document(postId).update(updates).await()
            
            // Refresh local cache
            refreshPost(postId)

            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Failed to update post")
        }
    }

    private fun mapSnapshotToPosts(snapshot: com.google.firebase.firestore.QuerySnapshot): List<Post> {
        return snapshot.documents.mapNotNull { doc ->
            mapDocumentToPost(doc)
        }
    }

    private fun mapDocumentToPost(doc: com.google.firebase.firestore.DocumentSnapshot): Post? {
        if (!doc.exists()) return null
        val userId = when (val ref = doc.get("userId")) {
            is com.google.firebase.firestore.DocumentReference -> ref.id
            is String -> ref
            else -> ""
        }
        return Post(
            id = doc.id,
            title = doc.getString("title") ?: "",
            description = doc.getString("description") ?: "",
            categoryId = doc.getString("categoryId") ?: "",
            cityId = (doc.getLong("cityId") ?: 0L).toInt(),
            imageUrl = doc.getString("imageUrl") ?: "",
            isTaken = doc.getBoolean("isTaken") ?: false,
            userId = userId
        )
    }
}
