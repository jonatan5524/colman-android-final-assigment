package com.example.colman_android_final_assigment.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class User(
    @PrimaryKey val id: String = "",
    val name: String = "",
    val email: String = "",
    val phone: String = "",
    val avatarUrl: String = ""
)
