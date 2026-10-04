package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.local.entity.CategoryBudgetEntity
import com.example.data.local.entity.CustomCategoryEntity
import com.example.data.local.entity.SavingsGoalEntity
import com.example.data.local.entity.TransactionEntity
import com.example.data.local.entity.UserEntity
import com.example.data.repository.SpendRepository
import com.example.ui.model.CategoryConstants
import com.example.ui.model.CategorySummary
import com.example.ui.model.CurrencyHelper
import com.example.ui.model.InsightType
import com.example.ui.model.SpendingInsight
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class ParsedReceipt(
    val title: String,
    val amount: Double,
    val category: String,
    val date: Long,
    val paymentMethod: String
)

class SpendViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: SpendRepository

    init {
        val db = AppDatabase.getInstance(application)
        repository = SpendRepository(db)
        viewModelScope.launch {
            repository.seedInitialDataIfEmpty()
        }
    }

    val userProfile: StateFlow<UserEntity?> = repository.userProfile
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val allTransactions: StateFlow<List<TransactionEntity>> = repository.allTransactions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recentTransactions: StateFlow<List<TransactionEntity>> = repository.recentTransactions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val savingsGoals: StateFlow<List<SavingsGoalEntity>> = repository.allSavingsGoals
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val customCategories: StateFlow<List<CustomCategoryEntity>> = repository.customCategories
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Month key for budgets
    private val currentMonthKey = CurrencyHelper.getYearMonthKey(System.currentTimeMillis())
    val categoryBudgets: StateFlow<List<CategoryBudgetEntity>> = repository.getBudgetsForMonth(currentMonthKey)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Filters for transactions screen
    val searchQuery = MutableStateFlow("")
    val filterType = MutableStateFlow("ALL") // "ALL", "INCOME", "EXPENSE"
    val filterCategory = MutableStateFlow<String?>(null)

    val filteredTransactions: StateFlow<List<TransactionEntity>> = combine(
        allTransactions,
        searchQuery,
        filterType,
        filterCategory
    ) { txs, query, type, cat ->
        txs.filter { tx ->
            val matchesQuery = query.isBlank() ||
                    tx.title.contains(query, ignoreCase = true) ||
                    tx.category.contains(query, ignoreCase = true) ||
                    tx.notes.contains(query, ignoreCase = true)

            val matchesType = when (type) {
                "INCOME" -> tx.type == "INCOME"
                "EXPENSE" -> tx.type == "EXPENSE"
                else -> true
            }

            val matchesCat = cat == null || tx.category.equals(cat, ignoreCase = true)

            matchesQuery && matchesType && matchesCat
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Aggregates for Dashboard
    val totalIncome: StateFlow<Double> = allTransactions.combine(MutableStateFlow(Unit)) { txs, _ ->
        txs.filter { it.type == "INCOME" }.sumOf { it.amount }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val totalExpenses: StateFlow<Double> = allTransactions.combine(MutableStateFlow(Unit)) { txs, _ ->
        txs.filter { it.type == "EXPENSE" }.sumOf { it.amount }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val totalBalance: StateFlow<Double> = combine(totalIncome, totalExpenses) { inc, exp ->
        inc - exp
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    // Current month expenses
    val currentMonthExpenses: StateFlow<Double> = allTransactions.combine(MutableStateFlow(Unit)) { txs, _ ->
        val cal = Calendar.getInstance()
        val currentMonth = cal.get(Calendar.MONTH)
        val currentYear = cal.get(Calendar.YEAR)

        txs.filter { tx ->
            if (tx.type != "EXPENSE") return@filter false
            val txCal = Calendar.getInstance().apply { timeInMillis = tx.date }
            txCal.get(Calendar.MONTH) == currentMonth && txCal.get(Calendar.YEAR) == currentYear
        }.sumOf { it.amount }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    // Category Summaries for current month
    val categorySummaries: StateFlow<List<CategorySummary>> = combine(
        allTransactions,
        categoryBudgets
    ) { txs, budgets ->
        val cal = Calendar.getInstance()
        val curMonth = cal.get(Calendar.MONTH)
        val curYear = cal.get(Calendar.YEAR)

        val expensesThisMonth = txs.filter {
            if (it.type != "EXPENSE") return@filter false
            val txCal = Calendar.getInstance().apply { timeInMillis = it.date }
            txCal.get(Calendar.MONTH) == curMonth && txCal.get(Calendar.YEAR) == curYear
        }

        val totalSpent = expensesThisMonth.sumOf { it.amount }.coerceAtLeast(1.0)
        val grouped = expensesThisMonth.groupBy { it.category }

        val list = grouped.map { (cat, list) ->
            val sum = list.sumOf { it.amount }
            val budget = budgets.find { it.category.equals(cat, ignoreCase = true) }?.budgetAmount
            CategorySummary(
                category = cat,
                emoji = CategoryConstants.getIconForCategory(cat, true),
                totalAmount = sum,
                percentage = ((sum / totalSpent) * 100).toFloat(),
                budgetAmount = budget,
                colorHex = CategoryConstants.getColorForCategory(cat)
            )
        }.sortedByDescending { it.totalAmount }

        if (list.isEmpty()) {
            emptyList()
        } else {
            list
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Smart Spending Insights derived from real data
    val smartInsights: StateFlow<List<SpendingInsight>> = combine(
        allTransactions,
        categorySummaries,
        userProfile
    ) { txs, catSummaries, user ->
        val insights = mutableListOf<SpendingInsight>()
        val currency = user?.currency ?: "₹"
        val monthlyBudget = user?.monthlyBudget ?: 10000.0

        val cal = Calendar.getInstance()
        val curDayOfMonth = cal.get(Calendar.DAY_OF_MONTH).coerceAtLeast(1)
        val curMonth = cal.get(Calendar.MONTH)
        val curYear = cal.get(Calendar.YEAR)

        val thisMonthExpenses = txs.filter {
            if (it.type != "EXPENSE") return@filter false
            val txCal = Calendar.getInstance().apply { timeInMillis = it.date }
            txCal.get(Calendar.MONTH) == curMonth && txCal.get(Calendar.YEAR) == curYear
        }
        val spentThisMonth = thisMonthExpenses.sumOf { it.amount }

        // Top category insight
        val topCategory = catSummaries.firstOrNull()
        if (topCategory != null) {
            insights.add(
                SpendingInsight(
                    id = "top_cat",
                    title = "Highest Spending",
                    message = "${topCategory.category} is your highest spending category at ${CurrencyHelper.formatPlain(topCategory.totalAmount, currency)} (${topCategory.percentage.toInt()}% of total expenses).",
                    type = InsightType.INFO,
                    emoji = topCategory.emoji
                )
            )
        }

        // Food category insight specifically for students
        val foodSummary = catSummaries.find { it.category.equals("Food", ignoreCase = true) }
        if (foodSummary != null) {
            val potentialSaving = (foodSummary.totalAmount * 0.20).toInt()
            insights.add(
                SpendingInsight(
                    id = "food_saving",
                    title = "Campus Food Tip",
                    message = "You spent ${CurrencyHelper.formatPlain(foodSummary.totalAmount, currency)} on food this month. You could save around $currency$potentialSaving by opting for campus meal plans or home-cooked snacks!",
                    type = InsightType.SAVINGS_TIP,
                    emoji = "💡"
                )
            )
        }

        // Daily average spending
        if (spentThisMonth > 0) {
            val dailyAvg = spentThisMonth / curDayOfMonth
            insights.add(
                SpendingInsight(
                    id = "daily_avg",
                    title = "Daily Spending Pace",
                    message = "Your average daily spending is ${CurrencyHelper.formatPlain(dailyAvg, currency)} per day.",
                    type = InsightType.INFO,
                    emoji = "⏱️"
                )
            )
        }

        // Monthly budget health
        if (monthlyBudget > 0) {
            val ratio = spentThisMonth / monthlyBudget
            if (ratio >= 1.0) {
                val over = spentThisMonth - monthlyBudget
                insights.add(
                    SpendingInsight(
                        id = "budget_over",
                        title = "Budget Exceeded",
                        message = "🚨 You have exceeded your monthly budget by ${CurrencyHelper.formatPlain(over, currency)}.",
                        type = InsightType.WARNING,
                        emoji = "🚨"
                    )
                )
            } else if (ratio >= 0.8) {
                insights.add(
                    SpendingInsight(
                        id = "budget_80",
                        title = "80% Budget Reached",
                        message = "⚠️ You have used ${(ratio * 100).toInt()}% of your monthly budget. Watch out for discretionary spends!",
                        type = InsightType.WARNING,
                        emoji = "⚠️"
                    )
                )
            } else {
                val left = monthlyBudget - spentThisMonth
                insights.add(
                    SpendingInsight(
                        id = "budget_good",
                        title = "On Track",
                        message = "🎉 You have ${CurrencyHelper.formatPlain(left, currency)} left in your budget for this month.",
                        type = InsightType.SUCCESS,
                        emoji = "🎯"
                    )
                )
            }
        }

        // Last month comparison
        val prevMonthCal = Calendar.getInstance().apply { add(Calendar.MONTH, -1) }
        val prevMonth = prevMonthCal.get(Calendar.MONTH)
        val prevYear = prevMonthCal.get(Calendar.YEAR)
        val lastMonthSpent = txs.filter {
            if (it.type != "EXPENSE") return@filter false
            val txCal = Calendar.getInstance().apply { timeInMillis = it.date }
            txCal.get(Calendar.MONTH) == prevMonth && txCal.get(Calendar.YEAR) == prevYear
        }.sumOf { it.amount }

        if (lastMonthSpent > 0 && spentThisMonth > 0) {
            val diff = spentThisMonth - lastMonthSpent
            val diffPercent = (kotlin.math.abs(diff) / lastMonthSpent * 100).toInt()
            if (diff < 0) {
                insights.add(
                    SpendingInsight(
                        id = "month_compare",
                        title = "Month-over-Month Win",
                        message = "You spent $diffPercent% less than last month! Fantastic work managing your expenses.",
                        type = InsightType.SUCCESS,
                        emoji = "🎉"
                    )
                )
            } else {
                insights.add(
                    SpendingInsight(
                        id = "month_compare",
                        title = "Spending Trend",
                        message = "Spending is $diffPercent% higher than last month. Check entertainment and shopping for quick trims.",
                        type = InsightType.INFO,
                        emoji = "📈"
                    )
                )
            }
        }

        insights
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Weekly/daily bars for Chart (last 7 days)
    val last7DaysSpending: StateFlow<List<Pair<String, Double>>> = allTransactions.combine(MutableStateFlow(Unit)) { txs, _ ->
        val result = mutableListOf<Pair<String, Double>>()
        val dayFormat = SimpleDateFormat("EEE", Locale.getDefault())
        val cal = Calendar.getInstance()

        for (i in 6 downTo 0) {
            val dayCal = Calendar.getInstance().apply {
                timeInMillis = cal.timeInMillis - (i * 86400000L)
            }
            val startOfDay = dayCal.apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis

            val endOfDay = startOfDay + 86400000L

            val spentThatDay = txs.filter {
                it.type == "EXPENSE" && it.date in startOfDay until endOfDay
            }.sumOf { it.amount }

            val label = dayFormat.format(Date(startOfDay))
            result.add(Pair(label, spentThatDay))
        }
        result
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Actions
    fun saveExpense(
        title: String,
        amount: Double,
        category: String,
        date: Long,
        paymentMethod: String,
        notes: String,
        receiptUri: String? = null,
        id: Long = 0L
    ) {
        viewModelScope.launch {
            val entity = TransactionEntity(
                id = id,
                type = "EXPENSE",
                amount = amount,
                title = title.ifBlank { category },
                category = category,
                date = date,
                paymentMethod = paymentMethod,
                notes = notes,
                receiptUri = receiptUri
            )
            repository.saveTransaction(entity)
        }
    }

    fun saveIncome(
        title: String,
        amount: Double,
        category: String,
        date: Long,
        paymentMethod: String,
        notes: String,
        id: Long = 0L
    ) {
        viewModelScope.launch {
            val entity = TransactionEntity(
                id = id,
                type = "INCOME",
                amount = amount,
                title = title.ifBlank { category },
                category = category,
                date = date,
                paymentMethod = paymentMethod,
                notes = notes
            )
            repository.saveTransaction(entity)
        }
    }

    fun deleteTransaction(transaction: TransactionEntity) {
        viewModelScope.launch {
            repository.deleteTransaction(transaction)
        }
    }

    fun updateMonthlyBudget(amount: Double) {
        viewModelScope.launch {
            repository.setMonthlyBudget(amount)
        }
    }

    fun setCategoryBudget(category: String, amount: Double) {
        viewModelScope.launch {
            repository.setCategoryBudget(category, amount, currentMonthKey)
        }
    }

    fun deleteCategoryBudget(id: Long) {
        viewModelScope.launch {
            repository.deleteCategoryBudget(id)
        }
    }

    fun saveSavingsGoal(title: String, targetAmount: Double, savedAmount: Double, emoji: String, id: Long = 0L) {
        viewModelScope.launch {
            val goal = SavingsGoalEntity(
                id = id,
                title = title,
                targetAmount = targetAmount,
                savedAmount = savedAmount,
                emoji = emoji
            )
            repository.saveSavingsGoal(goal)
        }
    }

    fun addMoneyToGoal(goalId: Long, amount: Double) {
        viewModelScope.launch {
            repository.addMoneyToGoal(goalId, amount)
        }
    }

    fun deleteSavingsGoal(goalId: Long) {
        viewModelScope.launch {
            repository.deleteSavingsGoal(goalId)
        }
    }

    fun updateUserProfile(name: String, email: String, currency: String, budget: Double) {
        viewModelScope.launch {
            val current = userProfile.value ?: UserEntity()
            repository.saveUserProfile(
                current.copy(
                    name = name,
                    email = email,
                    currency = currency,
                    monthlyBudget = budget
                )
            )
        }
    }

    fun setDarkMode(mode: Boolean?) {
        viewModelScope.launch {
            repository.setDarkMode(mode)
        }
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            repository.setNotificationsEnabled(enabled)
        }
    }

    fun completeOnboarding(name: String, budget: Double?) {
        viewModelScope.launch {
            val current = userProfile.value ?: UserEntity()
            repository.saveUserProfile(
                current.copy(
                    name = name.ifBlank { "Student" },
                    monthlyBudget = budget ?: 10000.0,
                    onboardingCompleted = true
                )
            )
        }
    }

    fun addCustomCategory(name: String, icon: String, isExpense: Boolean) {
        viewModelScope.launch {
            repository.addCustomCategory(name, icon, isExpense)
        }
    }

    suspend fun getCsvContent(): String {
        return repository.generateCsvData()
    }

    // Heuristic & Smart Receipt Parser
    fun parseReceiptSimulation(fileName: String? = null, hintText: String? = null): ParsedReceipt {
        val now = System.currentTimeMillis()
        val text = (hintText ?: "").lowercase()
        return when {
            text.contains("book") || text.contains("stationery") || text.contains("library") -> {
                ParsedReceipt("Campus Bookstore", 340.0, "Education", now, "UPI")
            }
            text.contains("coffee") || text.contains("cafe") || text.contains("starbucks") -> {
                ParsedReceipt("Campus Cafe", 180.0, "Food", now, "UPI")
            }
            text.contains("canteen") || text.contains("mess") || text.contains("pizza") || text.contains("burger") -> {
                ParsedReceipt("Student Canteen", 150.0, "Food", now, "UPI")
            }
            text.contains("uber") || text.contains("ola") || text.contains("metro") || text.contains("auto") -> {
                ParsedReceipt("Metro / Cab Ride", 95.0, "Transport", now, "UPI")
            }
            text.contains("pharmacy") || text.contains("med") -> {
                ParsedReceipt("City Chemist", 220.0, "Health", now, "Cash")
            }
            else -> {
                ParsedReceipt("Campus Mart", 250.0, "Shopping", now, "UPI")
            }
        }
    }
}
