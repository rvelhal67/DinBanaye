package com.dinbanaye.app.data.repository

import com.dinbanaye.app.data.database.CategoryDao
import com.dinbanaye.app.data.model.CategoryEntity
import kotlinx.coroutines.flow.Flow

class CategoryRepository(
    private val categoryDao: CategoryDao
) {

    val allCategories: Flow<List<CategoryEntity>> =
        categoryDao.getAllCategories()

    suspend fun getCategoryById(categoryId: Int): CategoryEntity? =
        categoryDao.getCategoryById(categoryId)

    suspend fun addCategory(category: CategoryEntity) =
        categoryDao.insertCategory(category)

    suspend fun updateCategory(category: CategoryEntity) =
        categoryDao.updateCategory(category)

    suspend fun deleteAllCategories() =
        categoryDao.deleteAllCategories()
}
