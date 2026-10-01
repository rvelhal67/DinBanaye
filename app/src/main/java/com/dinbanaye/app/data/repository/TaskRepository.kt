package com.dinbanaye.app.data.repository

import com.dinbanaye.app.data.database.TaskDao
import com.dinbanaye.app.data.model.TaskEntity
import kotlinx.coroutines.flow.Flow

class TaskRepository(
    private val taskDao: TaskDao
) {

    val allTasks: Flow<List<TaskEntity>> =
        taskDao.getAllTasks()

    suspend fun getTaskById(taskId: Long): TaskEntity? =
        taskDao.getTaskById(taskId)

    suspend fun addTask(task: TaskEntity): Long =
        taskDao.insertTask(task)

    suspend fun updateTask(task: TaskEntity) =
        taskDao.updateTask(task)

    suspend fun deleteTask(task: TaskEntity) =
        taskDao.deleteTask(task)

    suspend fun deleteAllTasks() =
        taskDao.deleteAllTasks()
}
