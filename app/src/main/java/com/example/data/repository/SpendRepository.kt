package com.example.data.repository

import com.example.data.local.AppDatabase
import com.example.data.local.entity.CategoryBudgetEntity
import com.example.data.local.entity.CustomCategoryEntity
import com.example.data.local.entity.SavingsGoalEntity
import com.example.data.local.entity.TransactionEntity
import com.example.data.local.entity.UserEntity
import com.example.ui.model.CurrencyHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SpendRepository(private val database: AppDatabase) {
    private val transactionDao = database.transactionDao()
    private val userDao = database.userDao()
    private val budgetDao = database.budgetDao()
    private val savingsGoalDao = database.savingsGoalDao()
    private val customCategoryDao = database.customCategoryDao()

    val allTransactions: Flow<List<TransactionEntity>> = transactionDao.getAllTransactions()
    val recentTransactions: Flow<List<TransactionEntity>> = transactionDao.getRecentTransactions(5)
    val userProfile: Flow<UserEntity?> = userDao.getUser()
    val allSavingsGoals: Flow<List<SavingsGoalEntity>> = savingsGoalDao.getAllGoals()
    val customCategories: Flow<List<CustomCategoryEntity>> = customCategoryDao.getAllCustomCategories()

    fun getBudgetsForMonth(monthKey: String): Flow<List<CategoryBudgetEntity>> {
        return budgetDao.getBudgetsForMonth(monthKey)
    }

    suspend fun getTransactionById(id: Long): TransactionEntity? {
        return withContext(Dispatchers.IO) {
            transactionDao.getTransactionByIdSync(id)
        }
    }

    suspend fun saveTransaction(transaction: TransactionEntity): Long {
        return withContext(Dispatchers.IO) {
            if (transaction.id == 0L) {
                transactionDao.insertTransaction(transaction)
            } else {
                transactionDao.updateTransaction(transaction)
                transaction.id
            }
        }
    }

    suspend fun deleteTransaction(transaction: TransactionEntity) {
        withContext(Dispatchers.IO) {
            transactionDao.deleteTransaction(transaction)
        }
    }

    suspend fun deleteTransactionById(id: Long) {
        withContext(Dispatchers.IO) {
            transactionDao.deleteTransactionById(id)
        }
    }

    suspend fun saveUserProfile(user: UserEntity) {
        withContext(Dispatchers.IO) {
            userDao.insertOrUpdate(user)
        }
    }

    suspend fun setMonthlyBudget(amount: Double) {
        withContext(Dispatchers.IO) {
            val user = userDao.getUserSync() ?: UserEntity()
            userDao.insertOrUpdate(user.copy(monthlyBudget = amount))
        }
    }

    suspend fun setDarkMode(mode: Boolean?) {
        withContext(Dispatchers.IO) {
            val user = userDao.getUserSync() ?: UserEntity()
            userDao.insertOrUpdate(user.copy(darkMode = mode))
        }
    }

    suspend fun setNotificationsEnabled(enabled: Boolean) {
        withContext(Dispatchers.IO) {
            val user = userDao.getUserSync() ?: UserEntity()
            userDao.insertOrUpdate(user.copy(notificationsEnabled = enabled))
        }
    }

    suspend fun setCurrency(currency: String) {
        withContext(Dispatchers.IO) {
            val user = userDao.getUserSync() ?: UserEntity()
            userDao.insertOrUpdate(user.copy(currency = currency))
        }
    }

    suspend fun setCategoryBudget(category: String, amount: Double, monthKey: String) {
        withContext(Dispatchers.IO) {
            val existing = budgetDao.getCategoryBudget(category, monthKey)
            if (existing != null) {
                budgetDao.insertOrUpdate(existing.copy(budgetAmount = amount))
            } else {
                budgetDao.insertOrUpdate(
                    CategoryBudgetEntity(
                        category = category,
                        budgetAmount = amount,
                        monthYear = monthKey
                    )
                )
            }
        }
    }

    suspend fun deleteCategoryBudget(id: Long) {
        withContext(Dispatchers.IO) {
            budgetDao.deleteBudget(id)
        }
    }

    suspend fun saveSavingsGoal(goal: SavingsGoalEntity): Long {
        return withContext(Dispatchers.IO) {
            savingsGoalDao.insertOrUpdate(goal)
        }
    }

    suspend fun addMoneyToGoal(goalId: Long, addAmount: Double) {
        withContext(Dispatchers.IO) {
            val goal = savingsGoalDao.getGoalById(goalId)
            if (goal != null) {
                val updated = goal.copy(savedAmount = (goal.savedAmount + addAmount).coerceAtLeast(0.0))
                savingsGoalDao.insertOrUpdate(updated)
            }
        }
    }

    suspend fun deleteSavingsGoal(id: Long) {
        withContext(Dispatchers.IO) {
            savingsGoalDao.deleteGoal(id)
        }
    }

    suspend fun addCustomCategory(name: String, icon: String, isExpense: Boolean) {
        withContext(Dispatchers.IO) {
            customCategoryDao.insert(CustomCategoryEntity(name = name, icon = icon, isExpense = isExpense))
        }
    }

    suspend fun generateCsvData(): String {
        return withContext(Dispatchers.IO) {
            val txs = transactionDao.getAllTransactions().firstOrNull() ?: emptyList()
            val sb = java.lang.StringBuilder()
            sb.append("ID,Type,Title,Category,Amount,Date,Payment Method,Notes\n")
            val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
            for (tx in txs) {
                val dateStr = sdf.format(Date(tx.date))
                val escapedTitle = "\"${tx.title.replace("\"", "\"\"")}\""
                val escapedNotes = "\"${tx.notes.replace("\"", "\"\"")}\""
                sb.append("${tx.id},${tx.type},$escapedTitle,${tx.category},${tx.amount},$dateStr,${tx.paymentMethod},$escapedNotes\n")
            }
            sb.toString()
        }
    }

    suspend fun seedInitialDataIfEmpty() {
        withContext(Dispatchers.IO) {
            val existingUser = userDao.getUserSync()
            if (existingUser == null) {
                // Initial student profile
                val initialUser = UserEntity(
                    id = 1,
                    name = "Aarav Sharma",
                    email = "aarav.sharma@college.edu",
                    currency = "₹",
                    monthlyBudget = 10000.0,
                    darkMode = null,
                    notificationsEnabled = true,
                    onboardingCompleted = true,
                    isProUser = false
                )
                userDao.insertOrUpdate(initialUser)

                val now = System.currentTimeMillis()
                val currentMonthKey = CurrencyHelper.getYearMonthKey(now)

                // Category Budgets matching the prompt
                budgetDao.insertOrUpdate(CategoryBudgetEntity(category = "Food", budgetAmount = 2500.0, monthYear = currentMonthKey))
                budgetDao.insertOrUpdate(CategoryBudgetEntity(category = "Transport", budgetAmount = 1500.0, monthYear = currentMonthKey))
                budgetDao.insertOrUpdate(CategoryBudgetEntity(category = "Education", budgetAmount = 2000.0, monthYear = currentMonthKey))
                budgetDao.insertOrUpdate(CategoryBudgetEntity(category = "Entertainment", budgetAmount = 1000.0, monthYear = currentMonthKey))
                budgetDao.insertOrUpdate(CategoryBudgetEntity(category = "Shopping", budgetAmount = 1000.0, monthYear = currentMonthKey))

                // Default Savings Goal
                savingsGoalDao.insertOrUpdate(
                    SavingsGoalEntity(
                        title = "New Headphones",
                        targetAmount = 5000.0,
                        savedAmount = 2750.0,
                        emoji = "🎧"
                    )
                )

                // Transactions to match prompt numbers:
                // Income: ₹18,000 (Pocket Money)
                // Expenses: Food: ₹1,850, Transport: ₹900, Education: ₹1,200, Entertainment: ₹700, Shopping: ₹500, Other: ₹400 = ₹5,550 total
                // Balance = ₹18,000 - ₹5,550 = ₹12,450
                val dayMillis = 86400000L

                // Income
                transactionDao.insertTransaction(
                    TransactionEntity(
                        type = "INCOME",
                        amount = 18000.0,
                        title = "Monthly Allowance",
                        category = "Pocket Money",
                        date = now - 2 * dayMillis,
                        paymentMethod = "UPI",
                        notes = "From parents for the month"
                    )
                )

                // Recent Food item: 🍕 College Canteen, Food, Today, -₹120
                transactionDao.insertTransaction(
                    TransactionEntity(
                        type = "EXPENSE",
                        amount = 120.0,
                        title = "College Canteen",
                        category = "Food",
                        date = now,
                        paymentMethod = "UPI",
                        notes = "Lunch with classmates"
                    )
                )

                // More Food expenses totaling 1850 - 120 = 1730
                transactionDao.insertTransaction(
                    TransactionEntity(
                        type = "EXPENSE",
                        amount = 450.0,
                        title = "Hostel Mess Snacks & Cafe",
                        category = "Food",
                        date = now - dayMillis,
                        paymentMethod = "UPI",
                        notes = "Evening coffee & rolls"
                    )
                )
                transactionDao.insertTransaction(
                    TransactionEntity(
                        type = "EXPENSE",
                        amount = 1280.0,
                        title = "Grocery & Fruit Supply",
                        category = "Food",
                        date = now - 3 * dayMillis,
                        paymentMethod = "Debit Card",
                        notes = "Oats, fruits, and snacks"
                    )
                )

                // Transport: ₹900
                transactionDao.insertTransaction(
                    TransactionEntity(
                        type = "EXPENSE",
                        amount = 900.0,
                        title = "Monthly Metro Card Recharge",
                        category = "Transport",
                        date = now - 4 * dayMillis,
                        paymentMethod = "UPI",
                        notes = "College daily commute"
                    )
                )

                // Education: ₹1,200
                transactionDao.insertTransaction(
                    TransactionEntity(
                        type = "EXPENSE",
                        amount = 1200.0,
                        title = "Course Books & Stationery",
                        category = "Education",
                        date = now - 5 * dayMillis,
                        paymentMethod = "Debit Card",
                        notes = "Engineering notebook and reference textbook"
                    )
                )

                // Entertainment: ₹700
                transactionDao.insertTransaction(
                    TransactionEntity(
                        type = "EXPENSE",
                        amount = 700.0,
                        title = "Weekend Movie & Arcade",
                        category = "Entertainment",
                        date = now - 6 * dayMillis,
                        paymentMethod = "UPI",
                        notes = "Movie night with hostel friends"
                    )
                )

                // Shopping: ₹500
                transactionDao.insertTransaction(
                    TransactionEntity(
                        type = "EXPENSE",
                        amount = 500.0,
                        title = "College T-Shirt & Pen Drive",
                        category = "Shopping",
                        date = now - 7 * dayMillis,
                        paymentMethod = "UPI",
                        notes = "Tech fest merch"
                    )
                )

                // Other: ₹400
                transactionDao.insertTransaction(
                    TransactionEntity(
                        type = "EXPENSE",
                        amount = 400.0,
                        title = "Mobile Data Pack",
                        category = "Other",
                        date = now - 8 * dayMillis,
                        paymentMethod = "UPI",
                        notes = "Unlimited 5G pack"
                    )
                )
            }
        }
    }
}
