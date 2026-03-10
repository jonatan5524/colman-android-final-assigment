package com.example.colman_android_final_assigment.data.local

import android.content.Context
import androidx.room.Room

object LocalDatabaseManager {

    @Volatile
    private var instance: AppLocalDb? = null

    fun getDatabase(context: Context): AppLocalDb {
        return instance ?: synchronized(this) {
            instance ?: buildDatabase(context.applicationContext).also { instance = it }
        }
    }

    private fun buildDatabase(context: Context): AppLocalDb {
        return Room.databaseBuilder(
            context,
            AppLocalDb::class.java,
            "giveaway_db"
        ).build()
    }
}
