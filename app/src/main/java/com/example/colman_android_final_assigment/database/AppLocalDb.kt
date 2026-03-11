package com.example.colman_android_final_assigment.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.colman_android_final_assigment.dao.PostDao
import com.example.colman_android_final_assigment.dao.UserDao
import com.example.colman_android_final_assigment.model.Post
import com.example.colman_android_final_assigment.model.User

@Database(entities = [User::class, Post::class], version = 2)
abstract class AppLocalDb : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun postDao(): PostDao

    companion object {
        @Volatile
        private var INSTANCE: AppLocalDb? = null

        fun getDatabase(context: Context): AppLocalDb {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppLocalDb::class.java,
                    "giveaway_db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
