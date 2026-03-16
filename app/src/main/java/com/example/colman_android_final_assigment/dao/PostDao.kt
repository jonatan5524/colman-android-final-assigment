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

    @Query("SELECT * FROM posts WHERE id = :postId")
    fun getPostById(postId: String): LiveData<Post?>

    @Query("SELECT COUNT(*) FROM posts WHERE userId = :userId")
    fun getPostCountByUser(userId: String): LiveData<Int>

    @Query("SELECT COUNT(*) FROM posts WHERE userId = :userId AND isTaken = 1")
    fun getGivenCountByUser(userId: String): LiveData<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(posts: List<Post>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPost(post: Post)

    @Delete
    suspend fun deletePost(post: Post)

    @Query("DELETE FROM posts")
    suspend fun clearAll()

    @Query("DELETE FROM posts WHERE userId = :userId AND id NOT IN (:postIds)")
    suspend fun deleteUserPostsNotInList(userId: String, postIds: List<String>)

    @Query("DELETE FROM posts WHERE userId = :userId")
    suspend fun deletePostsByUserId(userId: String)

    /** Combined search + multi-select categoryId AND city filter */
    @Query("""
        SELECT * FROM posts 
        WHERE (LOWER(title) LIKE '%' || LOWER(:query) || '%' 
            OR LOWER(description) LIKE '%' || LOWER(:query) || '%')
        AND (:noCategories = 1 OR categoryId IN (:categoryIds))
        AND (:noCities = 1 OR cityId IN (:cityIds))
        ORDER BY title ASC
    """)
    fun searchAndFilterMulti(
        query: String,
        categoryIds: List<String>,
        noCategories: Int,
        cityIds: List<Int>,
        noCities: Int
    ): LiveData<List<Post>>

    /** All unique category IDs currently in the cache */
    @Query("SELECT DISTINCT categoryId FROM posts WHERE categoryId != '' ORDER BY categoryId ASC")
    fun getAllCategoryIds(): LiveData<List<String>>

    /** All unique city IDs currently in the cache */
    @Query("SELECT DISTINCT cityId FROM posts ORDER BY cityId ASC")
    fun getAllCityIds(): LiveData<List<Int>>
}
