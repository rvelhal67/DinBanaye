package com.dinbanaye.app.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "task_categories")
data class CategoryEntity(
    @PrimaryKey
    val id: Int,

    val name: String,
    val color: Long
)
