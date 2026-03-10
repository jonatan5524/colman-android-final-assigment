package com.example.colman_android_final_assigment.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "posts")
data class Post(
    @PrimaryKey val id: String,
    val title: String,
    val description: String,
    val imageUrl: String,
    val city: String,
    val category: String,
    val whatsappNumber: String,
    val ownerId: String,
    val ownerName: String,
    val ownerImageUrl: String?,
    val lastUpdated: Long
)
