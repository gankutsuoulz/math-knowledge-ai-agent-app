package com.mathkatex.verify.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "favorites")
data class FavoriteEntity(
    @PrimaryKey val exampleId: String,
    val createdAt: Long,
)
