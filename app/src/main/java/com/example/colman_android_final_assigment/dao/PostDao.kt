package com.example.colman_android_final_assigment.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.example.colman_android_final_assigment.model.Post

@Dao
interface PostDao {
    @Query("SELECT * FROM posts ORDER BY title ASC")
    fun getAllPosts(): LiveData<List<Post>>

    @Query("SELECT * FROM posts WHERE userId = :userId ORDER BY title ASC")
    fun getPostsByUser(userId: String): LiveData<List<Post>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(posts: List<Post>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPost(post: Post)

    @Delete
    suspend fun deletePost(post: Post)

    @Query("DELETE FROM posts")
    suspend fun clearAll()

    /** Combined search + optional category/city filter */
    @Query("""
        SELECT * FROM posts 
        WHERE (LOWER(title) LIKE '%' || LOWER(:query) || '%' 
            OR LOWER(description) LIKE '%' || LOWER(:query) || '%')
        AND (:category IS NULL OR category = :category)
        AND (:cityId IS NULL OR cityId = :cityId)
        ORDER BY title ASC
    """)
    fun searchAndFilter(query: String, category: String?, cityId: Int?): LiveData<List<Post>>

    /** All unique categories currently in the cache */
    @Query("SELECT DISTINCT category FROM posts WHERE category != '' ORDER BY category ASC")
    fun getAllCategories(): LiveData<List<String>>

    /** All unique city IDs currently in the cache */
    @Query("SELECT DISTINCT cityId FROM posts ORDER BY cityId ASC")
    fun getAllCityIds(): LiveData<List<Int>>
}

