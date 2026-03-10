package com.example.colman_android_final_assigment.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.colman_android_final_assigment.data.local.dao.PostDao
import com.example.colman_android_final_assigment.data.local.entity.Post

@Database(entities = [Post::class], version = 1, exportSchema = false)
abstract class AppLocalDb : RoomDatabase() {
    abstract fun postDao(): PostDao
}
