package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.CategoryBudgetEntity
import com.example.data.local.entity.SavingsGoalEntity
import com.example.ui.components.AddSavingsGoalDialog
import com.example.ui.components.ContributeGoalDialog
import com.example.ui.components.DonutChart
import com.example.ui.components.InsightCard
import com.example.ui.components.SetCategoryBudgetDialog
import com.example.ui.components.SetMonthlyBudgetDialog
import com.example.ui.components.WeeklyBarChart
import com.example.ui.model.CategoryConstants
import com.example.ui.model.CategorySummary
import com.example.ui.model.CurrencyHelper
import com.example.ui.model.SpendingInsight
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.IncomeGreen
import com.example.ui.theme.PrimaryIndigo
import com.example.ui.theme.SecondaryViolet
import com.example.ui.theme.WarningAmber

@Composable
fun BudgetScreen(
    monthlyBudget: Double,
    spentThisMonth: Double,
    categoryBudgets: List<CategoryBudgetEntity>,
    categorySummaries: List<CategorySummary>,
    savingsGoals: List<SavingsGoalEntity>,
    insights: List<SpendingInsight>,
    dailySpending: List<Pair<String, Double>>,
    currencySymbol: String = "₹",
    onUpdateMonthlyBudget: (Double) -> Unit,
    onSetCategoryBudget: (category: String, amount: Double) -> Unit,
    onDeleteCategoryBudget: (id: Long) -> Unit,
    onCreateGoal: (title: String, target: Double, saved: Double, emoji: String) -> Unit,
    onAddMoneyToGoal: (goalId: Long, amount: Double) -> Unit,
    onDeleteGoal: (goalId: Long) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Budget & Goals, 1: Analytics & Insights

    var showMonthlyBudgetDialog by remember { mutableStateOf(false) }
    var categoryToEditBudget by remember { mutableStateOf<Pair<String, Double?>?>(null) }
    var showAddGoalDialog by remember { mutableStateOf(false) }
    var goalToContribute by remember { mutableStateOf<SavingsGoalEntity?>(null) }

    val currentMonth = CurrencyHelper.formatMonth()
    val remaining = monthlyBudget - spentThisMonth

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(8.dp))

        // Title Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "$currentMonth Budget",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            IconButton(
                onClick = { showMonthlyBudgetDialog = true },
                modifier = Modifier.testTag("edit_monthly_budget_icon")
            ) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Edit Budget",
                    tint = PrimaryIndigo
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Section Tabs
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                modifier = Modifier.testTag("tab_budgets_goals")
            ) {
                Text(
                    text = "Budgets & Goals",
                    modifier = Modifier.padding(vertical = 10.dp),
                    fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal,
                    color = if (selectedTab == 0) PrimaryIndigo else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                modifier = Modifier.testTag("tab_analytics_insights")
            ) {
                Text(
                    text = "Analytics & Insights 💡",
                    modifier = Modifier.padding(vertical = 10.dp),
                    fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal,
                    color = if (selectedTab == 1) PrimaryIndigo else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        if (selectedTab == 0) {
            // Budgets & Savings Goals Tab
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                // Monthly Budget Master Card
                item {
                    val progress = if (monthlyBudget > 0) (spentThisMonth / monthlyBudget).toFloat().coerceIn(0f, 1f) else 0f
                    val isExceeded = spentThisMonth > monthlyBudget && monthlyBudget > 0

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(22.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Monthly Budget",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = CurrencyHelper.formatPlain(monthlyBudget, currencySymbol),
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = PrimaryIndigo
                                )
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            LinearProgressIndicator(
                                progress = { progress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(10.dp)
                                    .clip(RoundedCornerShape(5.dp)),
                                color = if (isExceeded) ExpenseRed else if (progress >= 0.8f) WarningAmber else PrimaryIndigo,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant
                            )

                            Spacer(modifier = Modifier.height(14.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text("Spent", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(
                                        text = CurrencyHelper.formatPlain(spentThisMonth, currencySymbol),
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isExceeded) ExpenseRed else MaterialTheme.colorScheme.onSurface
                                    )
                                }

                                Column(horizontalAlignment = Alignment.End) {
                                    Text("Remaining", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(
                                        text = CurrencyHelper.formatPlain(remaining, currencySymbol),
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (remaining < 0) ExpenseRed else IncomeGreen
                                    )
                                }
                            }
                        }
                    }
                }

                // Category Budgets Header
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Category Budgets",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        TextButton(
                            onClick = {
                                categoryToEditBudget = Pair("Food", 2500.0)
                            }
                        ) {
                            Text("+ Add Limit", color = PrimaryIndigo, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }

                // Category Budgets List
                val defaultExpenseCategories = CategoryConstants.DEFAULT_EXPENSE_CATEGORIES.map { it.name }
                val activeCategories = (defaultExpenseCategories + categorySummaries.map { it.category }).distinct()

                items(activeCategories) { categoryName ->
                    val summary = categorySummaries.find { it.category.equals(categoryName, ignoreCase = true) }
                    val spent = summary?.totalAmount ?: 0.0
                    val budgetEntity = categoryBudgets.find { it.category.equals(categoryName, ignoreCase = true) }
                    val budgetAmount = budgetEntity?.budgetAmount

                    val icon = CategoryConstants.getIconForCategory(categoryName, true)

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                categoryToEditBudget = Pair(categoryName, budgetAmount)
                            },
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(text = icon, fontSize = 20.sp)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = categoryName,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (budgetAmount != null && budgetAmount > 0) {
                                        Text(
                                            text = "${CurrencyHelper.formatPlain(spent, currencySymbol)} / ${CurrencyHelper.formatPlain(budgetAmount, currencySymbol)}",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    } else {
                                        Text(
                                            text = "${CurrencyHelper.formatPlain(spent, currencySymbol)} spent (No limit)",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }

                            if (budgetAmount != null && budgetAmount > 0) {
                                val catProgress = (spent / budgetAmount).toFloat().coerceIn(0f, 1f)
                                val isCatExceeded = spent > budgetAmount
                                val isNearLimit = spent >= (0.8 * budgetAmount) && !isCatExceeded

                                Spacer(modifier = Modifier.height(10.dp))

                                LinearProgressIndicator(
                                    progress = { catProgress },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp)
                                        .clip(RoundedCornerShape(3.dp)),
                                    color = if (isCatExceeded) ExpenseRed else if (isNearLimit) WarningAmber else PrimaryIndigo,
                                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                                )

                                Spacer(modifier = Modifier.height(6.dp))

                                // Alert Warnings
                                if (isCatExceeded) {
                                    val over = spent - budgetAmount
                                    Text(
                                        text = "🚨 $categoryName budget exceeded by ${CurrencyHelper.formatPlain(over, currencySymbol)}",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Bold,
                                        color = ExpenseRed
                                    )
                                } else if (isNearLimit) {
                                    Text(
                                        text = "⚠️ You are close to your $categoryName budget.",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Bold,
                                        color = WarningAmber
                                    )
                                }
                            }
                        }
                    }
                }

                // Savings Goals Section 🎯
                item {
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Savings Goals 🎯",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Button(
                            onClick = { showAddGoalDialog = true },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo),
                            modifier = Modifier.testTag("create_goal_button")
                        ) {
                            Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("New Goal")
                        }
                    }
                }

                if (savingsGoals.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("🎯", fontSize = 40.sp)
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Dream big. Start saving. 🎯",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Set a goal for new headphones, a course, or a trip.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(14.dp))
                                OutlinedButton(onClick = { showAddGoalDialog = true }) {
                                    Text("Create Goal")
                                }
                            }
                        }
                    }
                } else {
                    items(savingsGoals) { goal ->
                        val percent = if (goal.targetAmount > 0) {
                            ((goal.savedAmount / goal.targetAmount) * 100).toInt().coerceIn(0, 100)
                        } else 0
                        val remainingGoal = (goal.targetAmount - goal.savedAmount).coerceAtLeast(0.0)

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(text = goal.emoji, fontSize = 24.sp)
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Text(
                                                text = goal.title,
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = "Target: ${CurrencyHelper.formatPlain(goal.targetAmount, currencySymbol)}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }

                                    IconButton(
                                        onClick = { onDeleteGoal(goal.id) },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Delete Goal",
                                            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.6f),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                LinearProgressIndicator(
                                    progress = { (percent / 100f).coerceIn(0f, 1f) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(8.dp)
                                        .clip(RoundedCornerShape(4.dp)),
                                    color = IncomeGreen,
                                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                                )

                                Spacer(modifier = Modifier.height(10.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = "Saved: ${CurrencyHelper.formatPlain(goal.savedAmount, currencySymbol)} ($percent%)",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Bold,
                                            color = IncomeGreen
                                        )
                                        Text(
                                            text = "Remaining: ${CurrencyHelper.formatPlain(remainingGoal, currencySymbol)}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    OutlinedButton(
                                        onClick = { goalToContribute = goal },
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Icon(imageVector = Icons.Default.Savings, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("+ Add Money", fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(80.dp))
                }
            }
        } else {
            // Analytics & Insights Tab
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                // Monthly spending bar chart
                item {
                    WeeklyBarChart(
                        dailyData = dailySpending,
                        currencySymbol = currencySymbol
                    )
                }

                // Category breakdown donut chart
                item {
                    DonutChart(
                        categories = categorySummaries,
                        totalSpent = spentThisMonth,
                        currencySymbol = currencySymbol
                    )
                }

                // Spending Insights Header
                item {
                    Text(
                        text = "Your Spending Insights 💡",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }

                if (insights.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Text(
                                text = "Keep adding expenses to unlock personalized spending tips and insights.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    }
                } else {
                    items(insights) { insight ->
                        InsightCard(insight = insight)
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(80.dp))
                }
            }
        }
    }

    // Set Monthly Budget Dialog
    if (showMonthlyBudgetDialog) {
        SetMonthlyBudgetDialog(
            currentBudget = monthlyBudget,
            currencySymbol = currencySymbol,
            onDismiss = { showMonthlyBudgetDialog = false },
            onSave = { onUpdateMonthlyBudget(it) }
        )
    }

    // Set Category Budget Dialog
    if (categoryToEditBudget != null) {
        val (catName, currentLimit) = categoryToEditBudget!!
        SetCategoryBudgetDialog(
            category = catName,
            currentLimit = currentLimit,
            currencySymbol = currencySymbol,
            onDismiss = { categoryToEditBudget = null },
            onSave = {
                onSetCategoryBudget(catName, it)
                categoryToEditBudget = null
            }
        )
    }

    // Add Goal Dialog
    if (showAddGoalDialog) {
        AddSavingsGoalDialog(
            currencySymbol = currencySymbol,
            onDismiss = { showAddGoalDialog = false },
            onSave = { title, target, saved, emoji ->
                onCreateGoal(title, target, saved, emoji)
            }
        )
    }

    // Contribute Goal Dialog
    if (goalToContribute != null) {
        ContributeGoalDialog(
            goalTitle = goalToContribute!!.title,
            currencySymbol = currencySymbol,
            onDismiss = { goalToContribute = null },
            onAdd = {
                onAddMoneyToGoal(goalToContribute!!.id, it)
                goalToContribute = null
            }
        )
    }
}
