package com.example.colman_android_final_assigment.repository

import android.content.Context
import androidx.lifecycle.LiveData
import com.example.colman_android_final_assigment.base.Resource
import com.example.colman_android_final_assigment.database.AppLocalDb
import com.example.colman_android_final_assigment.model.Category
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class CategoryRepository(context: Context) {

    private val firestore get() = FirebaseFirestore.getInstance()
    private val categoryDao = AppLocalDb.getDatabase(context).categoryDao()

    /** LiveData of all categories from the local Room cache */
    fun getAllCategories(): LiveData<List<Category>> = categoryDao.getAllCategories()

    /**
     * Fetch all categories from Firestore and refresh the local Room cache.
     */
    suspend fun refreshCategories(): Resource<Unit> {
        return try {
            val snapshot = firestore.collection("categories").get().await()
            val categories = snapshot.documents.mapNotNull { doc ->
                Category(
                    id = doc.id,
                    name = doc.getString("name") ?: ""
                )
            }
            categoryDao.clearAll()
            categoryDao.insertAll(categories)
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Failed to refresh categories")
        }
    }
}
