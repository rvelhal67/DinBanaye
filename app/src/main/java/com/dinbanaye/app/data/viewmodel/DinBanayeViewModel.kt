package com.dinbanaye.app.data.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.dinbanaye.app.data.database.DinBanayeDatabase
import com.dinbanaye.app.data.model.CategoryEntity
import com.dinbanaye.app.data.model.TaskEntity
import com.dinbanaye.app.data.repository.CategoryRepository
import com.dinbanaye.app.data.repository.TaskRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class DinBanayeViewModel(application: Application) : AndroidViewModel(application) {

    private val database = DinBanayeDatabase.getDatabase(application)

    private val taskRepository = TaskRepository(database.taskDao())
    private val categoryRepository = CategoryRepository(database.categoryDao())

    val tasks: StateFlow<List<TaskEntity>> =
        taskRepository.allTasks.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )

    val categories: StateFlow<List<CategoryEntity>> =
        categoryRepository.allCategories.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )

    fun addTask(task: TaskEntity) {
        viewModelScope.launch {
            taskRepository.addTask(task)
        }
    }

    fun updateTask(task: TaskEntity) {
        viewModelScope.launch {
            taskRepository.updateTask(task)
        }
    }

    fun deleteTask(task: TaskEntity) {
        viewModelScope.launch {
            taskRepository.deleteTask(task)
        }
    }

    fun addCategory(category: CategoryEntity) {
        viewModelScope.launch {
            categoryRepository.addCategory(category)
        }
    }

    fun updateCategory(category: CategoryEntity) {
        viewModelScope.launch {
            categoryRepository.updateCategory(category)
        }
    }
}
