package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.CategoryBudgetEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BudgetDao {
    @Query("SELECT * FROM category_budgets WHERE monthYear = :monthYear")
    fun getBudgetsForMonth(monthYear: String): Flow<List<CategoryBudgetEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(budget: CategoryBudgetEntity): Long

    @Query("SELECT * FROM category_budgets WHERE category = :category AND monthYear = :monthYear LIMIT 1")
    suspend fun getCategoryBudget(category: String, monthYear: String): CategoryBudgetEntity?

    @Query("DELETE FROM category_budgets WHERE id = :id")
    suspend fun deleteBudget(id: Long)
}
