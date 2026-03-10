package com.example.colman_android_final_assigment.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "posts")
data class Post(
    @PrimaryKey val id: String,
    val title: String,
    val description: String,
    val image: String,
    val category: String,
    val isTaken: Boolean,
    val cityId: Int,
    val userId: String,
    val userName: String,
    val userImageUrl: String,
    val userPhoneNumber: String,
    val lastUpdated: Long
)
