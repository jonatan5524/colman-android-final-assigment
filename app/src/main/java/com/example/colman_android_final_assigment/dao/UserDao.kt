package com.example.colman_android_final_assigment.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.example.colman_android_final_assigment.model.User

@Dao
interface UserDao {
    @Query("SELECT * FROM users WHERE id = :userId")
    fun getUserById(userId: String): LiveData<User?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: User)

    @Query("DELETE FROM users")
    suspend fun clearAll()
}
