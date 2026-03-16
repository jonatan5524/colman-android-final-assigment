package com.example.colman_android_final_assigment.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "posts")
data class Post(
    @PrimaryKey val id: String = "",
    val title: String = "",
    val description: String = "",
    val categoryId: String = "",
    val cityId: Int = 0,
    val imageUrl: String = "",
    val isTaken: Boolean = false,
    val userId: String = ""
)

