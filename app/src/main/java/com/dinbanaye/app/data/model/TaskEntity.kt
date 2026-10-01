package com.dinbanaye.app.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val title: String,
    val time: String,
    val section: String,
    val repeat: String = "Every day",
    val reminder: Boolean = false,
    val categoryId: Int = 14,
    val doneDates: Set<String> = emptySet()
)
