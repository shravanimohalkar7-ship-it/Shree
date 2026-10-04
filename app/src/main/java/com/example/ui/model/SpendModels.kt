package com.example.ui.model

import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class CategoryItem(
    val name: String,
    val icon: String,
    val colorHex: Long = 0xFF4F46E5
)

object CategoryConstants {
    val DEFAULT_EXPENSE_CATEGORIES = listOf(
        CategoryItem("Food", "🍔", 0xFFF97316),
        CategoryItem("Transport", "🚕", 0xFF0EA5E9),
        CategoryItem("Education", "📚", 0xFF8B5CF6),
        CategoryItem("Entertainment", "🎮", 0xFFEC4899),
        CategoryItem("Shopping", "🛍️", 0xFF10B981),
        CategoryItem("Hostel/Rent", "🏠", 0xFF6366F1),
        CategoryItem("Health", "💊", 0xFFEF4444),
        CategoryItem("Recharge", "📱", 0xFF14B8A6),
        CategoryItem("Other", "☕", 0xFF64748B)
    )

    val DEFAULT_INCOME_CATEGORIES = listOf(
        CategoryItem("Pocket Money", "💰", 0xFF10B981),
        CategoryItem("Salary", "💼", 0xFF3B82F6),
        CategoryItem("Scholarship", "🎓", 0xFF8B5CF6),
        CategoryItem("Freelance", "💻", 0xFFF59E0B),
        CategoryItem("Gift", "🎁", 0xFFEC4899),
        CategoryItem("Other", "💡", 0xFF64748B)
    )

    val PAYMENT_METHODS = listOf("UPI", "Cash", "Debit Card", "Credit Card", "Other")

    fun getIconForCategory(category: String, isExpense: Boolean = true): String {
        val clean = category.trim()
        val match = if (isExpense) {
            DEFAULT_EXPENSE_CATEGORIES.find { it.name.equals(clean, ignoreCase = true) }
        } else {
            DEFAULT_INCOME_CATEGORIES.find { it.name.equals(clean, ignoreCase = true) }
        }
        if (match != null) return match.icon
        // Fallback checks
        return when {
            clean.contains("Food", true) || clean.contains("Eat", true) || clean.contains("Lunch", true) || clean.contains("Dinner", true) -> "🍔"
            clean.contains("Transport", true) || clean.contains("Cab", true) || clean.contains("Metro", true) || clean.contains("Auto", true) -> "🚕"
            clean.contains("Book", true) || clean.contains("Study", true) || clean.contains("College", true) || clean.contains("Exam", true) -> "📚"
            clean.contains("Shop", true) || clean.contains("Clothes", true) -> "🛍️"
            clean.contains("Game", true) || clean.contains("Movie", true) || clean.contains("Party", true) -> "🎮"
            clean.contains("Hostel", true) || clean.contains("Rent", true) || clean.contains("Room", true) -> "🏠"
            clean.contains("Health", true) || clean.contains("Medicine", true) || clean.contains("Doctor", true) -> "💊"
            clean.contains("Recharge", true) || clean.contains("Wifi", true) || clean.contains("Phone", true) -> "📱"
            clean.contains("Coffee", true) || clean.contains("Tea", true) || clean.contains("Cafe", true) -> "☕"
            clean.contains("Pocket", true) -> "💰"
            clean.contains("Salary", true) -> "💼"
            clean.contains("Scholarship", true) -> "🎓"
            else -> if (isExpense) "🏷️" else "💵"
        }
    }

    fun getColorForCategory(category: String): Long {
        val clean = category.trim()
        val expenseMatch = DEFAULT_EXPENSE_CATEGORIES.find { it.name.equals(clean, ignoreCase = true) }
        if (expenseMatch != null) return expenseMatch.colorHex
        val incomeMatch = DEFAULT_INCOME_CATEGORIES.find { it.name.equals(clean, ignoreCase = true) }
        if (incomeMatch != null) return incomeMatch.colorHex
        return 0xFF6366F1
    }
}

enum class InsightType {
    INFO,
    WARNING,
    SUCCESS,
    SAVINGS_TIP
}

data class SpendingInsight(
    val id: String,
    val title: String,
    val message: String,
    val type: InsightType,
    val emoji: String
)

data class CategorySummary(
    val category: String,
    val emoji: String,
    val totalAmount: Double,
    val percentage: Float,
    val budgetAmount: Double? = null,
    val colorHex: Long = 0xFF4F46E5
)

object CurrencyHelper {
    fun formatAmount(amount: Double, currencySymbol: String = "₹"): String {
        val isNegative = amount < 0
        val abs = kotlin.math.abs(amount)
        val formatted = if (abs % 1.0 == 0.0) {
            String.format(Locale.getDefault(), "%,d", abs.toLong())
        } else {
            String.format(Locale.getDefault(), "%,.2f", abs)
        }
        return if (isNegative) "-$currencySymbol$formatted" else "$currencySymbol$formatted"
    }

    fun formatPlain(amount: Double, currencySymbol: String = "₹"): String {
        val abs = kotlin.math.abs(amount)
        val formatted = if (abs % 1.0 == 0.0) {
            String.format(Locale.getDefault(), "%,d", abs.toLong())
        } else {
            String.format(Locale.getDefault(), "%,.2f", abs)
        }
        return "$currencySymbol$formatted"
    }

    fun formatDate(timestamp: Long): String {
        val now = System.currentTimeMillis()
        val sdfDateOnly = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
        val isToday = sdfDateOnly.format(Date(timestamp)) == sdfDateOnly.format(Date(now))
        val yesterday = Date(now - 86400000L)
        val isYesterday = sdfDateOnly.format(Date(timestamp)) == sdfDateOnly.format(yesterday)

        return when {
            isToday -> "Today"
            isYesterday -> "Yesterday"
            else -> SimpleDateFormat("dd MMM, yyyy", Locale.getDefault()).format(Date(timestamp))
        }
    }

    fun formatMonth(timestamp: Long = System.currentTimeMillis()): String {
        return SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(Date(timestamp))
    }

    fun getYearMonthKey(timestamp: Long = System.currentTimeMillis()): String {
        return SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date(timestamp))
    }
}
