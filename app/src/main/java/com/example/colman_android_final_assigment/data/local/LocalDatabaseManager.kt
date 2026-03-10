package com.example.colman_android_final_assigment.data.local

import android.content.Context
import androidx.room.Room

object LocalDatabaseManager {

    @Volatile
    private var instance: AppLocalDb? = null

    fun getDatabase(context: Context): AppLocalDb {
        return instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                context.applicationContext,
                AppLocalDb::class.java,
                "giveaway_db"
            ).build().also { instance = it }
        }
    }
}
