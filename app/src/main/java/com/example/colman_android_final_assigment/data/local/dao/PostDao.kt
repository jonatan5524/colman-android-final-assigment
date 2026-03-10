package com.example.colman_android_final_assigment.data.local.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.colman_android_final_assigment.data.local.entity.Post

@Dao
interface PostDao {

    @Query("SELECT * FROM posts ORDER BY lastUpdated DESC")
    fun getAllPosts(): LiveData<List<Post>>

    @Query("SELECT * FROM posts WHERE userId = :userId ORDER BY lastUpdated DESC")
    fun getPostsByOwnerId(userId: String): LiveData<List<Post>>

    @Query("SELECT * FROM posts WHERE id = :postId")
    fun getPostById(postId: String): LiveData<Post?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(posts: List<Post>)

    @Query("DELETE FROM posts")
    suspend fun deleteAll()
}
