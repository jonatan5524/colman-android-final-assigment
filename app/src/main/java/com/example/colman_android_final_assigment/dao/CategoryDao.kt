package com.example.colman_android_final_assigment.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.example.colman_android_final_assigment.model.Category

@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories ORDER BY name ASC")
    fun getAllCategories(): LiveData<List<Category>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(categories: List<Category>)

    @Query("SELECT name FROM categories WHERE id = :categoryId LIMIT 1")
    suspend fun getCategoryNameById(categoryId: String): String?

    @Query("DELETE FROM categories")
    suspend fun clearAll()
}
