package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.PieChart
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.local.entity.TransactionEntity
import com.example.ui.components.AddEditTransactionSheet
import com.example.ui.components.SetMonthlyBudgetDialog
import com.example.ui.screens.BudgetScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.OnboardingScreen
import com.example.ui.screens.ProfileScreen
import com.example.ui.screens.TransactionsScreen
import com.example.ui.theme.PrimaryIndigo
import com.example.ui.theme.StudentSpendTheme
import com.example.ui.viewmodel.SpendViewModel

enum class MainTab(val label: String, val activeIcon: ImageVector, val inactiveIcon: ImageVector) {
    HOME("Home", Icons.Filled.Home, Icons.Outlined.Home),
    TRANSACTIONS("Transactions", Icons.Filled.ReceiptLong, Icons.Outlined.ReceiptLong),
    BUDGET("Budget", Icons.Filled.PieChart, Icons.Outlined.PieChart),
    PROFILE("Profile", Icons.Filled.Person, Icons.Outlined.Person)
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val spendViewModel: SpendViewModel = viewModel()
            val userProfile by spendViewModel.userProfile.collectAsStateWithLifecycle()

            val isDarkTheme = when (userProfile?.darkMode) {
                true -> true
                false -> false
                null -> isSystemInDarkTheme()
            }

            StudentSpendTheme(darkTheme = isDarkTheme) {
                if (userProfile != null && !userProfile!!.onboardingCompleted) {
                    OnboardingScreen(
                        onFinish = { name, budget ->
                            spendViewModel.completeOnboarding(name, budget)
                        }
                    )
                } else {
                    MainAppContent(viewModel = spendViewModel)
                }
            }
        }
    }
}

@Composable
fun MainAppContent(viewModel: SpendViewModel) {
    val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()
    val currency = userProfile?.currency ?: "₹"
    val monthlyBudget = userProfile?.monthlyBudget ?: 10000.0

    val totalBalance by viewModel.totalBalance.collectAsStateWithLifecycle()
    val totalIncome by viewModel.totalIncome.collectAsStateWithLifecycle()
    val totalExpenses by viewModel.totalExpenses.collectAsStateWithLifecycle()
    val currentMonthExpenses by viewModel.currentMonthExpenses.collectAsStateWithLifecycle()
    val categorySummaries by viewModel.categorySummaries.collectAsStateWithLifecycle()
    val recentTransactions by viewModel.recentTransactions.collectAsStateWithLifecycle()
    val allTransactions by viewModel.filteredTransactions.collectAsStateWithLifecycle()
    val categoryBudgets by viewModel.categoryBudgets.collectAsStateWithLifecycle()
    val savingsGoals by viewModel.savingsGoals.collectAsStateWithLifecycle()
    val insights by viewModel.smartInsights.collectAsStateWithLifecycle()
    val dailySpending by viewModel.last7DaysSpending.collectAsStateWithLifecycle()
    val customCategories by viewModel.customCategories.collectAsStateWithLifecycle()

    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val filterType by viewModel.filterType.collectAsStateWithLifecycle()
    val filterCategory by viewModel.filterCategory.collectAsStateWithLifecycle()

    var currentTab by remember { mutableStateOf(MainTab.HOME) }

    // Dialog & Sheet States
    var showAddTransactionSheet by remember { mutableStateOf(false) }
    var transactionSheetInitialType by remember { mutableStateOf("EXPENSE") }
    var transactionToEdit by remember { mutableStateOf<TransactionEntity?>(null) }
    var showSetMonthlyBudgetDialog by remember { mutableStateOf(false) }

    // BackHandler to return to HOME tab before exiting
    if (currentTab != MainTab.HOME) {
        BackHandler {
            currentTab = MainTab.HOME
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface
            ) {
                MainTab.values().forEach { tab ->
                    val isSelected = currentTab == tab
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = { currentTab = tab },
                        icon = {
                            Icon(
                                imageVector = if (isSelected) tab.activeIcon else tab.inactiveIcon,
                                contentDescription = tab.label
                            )
                        },
                        label = { Text(tab.label) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = PrimaryIndigo,
                            selectedTextColor = PrimaryIndigo,
                            indicatorColor = PrimaryIndigo.copy(alpha = 0.15f)
                        ),
                        modifier = Modifier.testTag("nav_tab_${tab.name.lowercase()}")
                    )
                }
            }
        }
    ) { innerPadding ->
        when (currentTab) {
            MainTab.HOME -> {
                HomeScreen(
                    user = userProfile,
                    totalBalance = totalBalance,
                    totalIncome = totalIncome,
                    totalExpenses = totalExpenses,
                    currentMonthExpenses = currentMonthExpenses,
                    monthlyBudget = monthlyBudget,
                    categorySummaries = categorySummaries,
                    recentTransactions = recentTransactions,
                    topInsight = insights.firstOrNull(),
                    currencySymbol = currency,
                    onAddExpense = {
                        transactionToEdit = null
                        transactionSheetInitialType = "EXPENSE"
                        showAddTransactionSheet = true
                    },
                    onAddIncome = {
                        transactionToEdit = null
                        transactionSheetInitialType = "INCOME"
                        showAddTransactionSheet = true
                    },
                    onSetBudget = { showSetMonthlyBudgetDialog = true },
                    onSeeAllTransactions = { currentTab = MainTab.TRANSACTIONS },
                    onEditTransaction = { tx ->
                        transactionToEdit = tx
                        transactionSheetInitialType = tx.type
                        showAddTransactionSheet = true
                    },
                    onDeleteTransaction = { tx ->
                        viewModel.deleteTransaction(tx)
                    },
                    modifier = Modifier.padding(innerPadding)
                )
            }

            MainTab.TRANSACTIONS -> {
                TransactionsScreen(
                    transactions = allTransactions,
                    searchQuery = searchQuery,
                    filterType = filterType,
                    selectedCategory = filterCategory,
                    currencySymbol = currency,
                    onSearchChange = { viewModel.searchQuery.value = it },
                    onFilterTypeChange = { viewModel.filterType.value = it },
                    onSelectCategory = { viewModel.filterCategory.value = it },
                    onAddExpense = {
                        transactionToEdit = null
                        transactionSheetInitialType = "EXPENSE"
                        showAddTransactionSheet = true
                    },
                    onEditTransaction = { tx ->
                        transactionToEdit = tx
                        transactionSheetInitialType = tx.type
                        showAddTransactionSheet = true
                    },
                    onDeleteTransaction = { tx ->
                        viewModel.deleteTransaction(tx)
                    },
                    modifier = Modifier.padding(innerPadding)
                )
            }

            MainTab.BUDGET -> {
                BudgetScreen(
                    monthlyBudget = monthlyBudget,
                    spentThisMonth = currentMonthExpenses,
                    categoryBudgets = categoryBudgets,
                    categorySummaries = categorySummaries,
                    savingsGoals = savingsGoals,
                    insights = insights,
                    dailySpending = dailySpending,
                    currencySymbol = currency,
                    onUpdateMonthlyBudget = { viewModel.updateMonthlyBudget(it) },
                    onSetCategoryBudget = { cat, amt -> viewModel.setCategoryBudget(cat, amt) },
                    onDeleteCategoryBudget = { id -> viewModel.deleteCategoryBudget(id) },
                    onCreateGoal = { title, target, saved, emoji ->
                        viewModel.saveSavingsGoal(title, target, saved, emoji)
                    },
                    onAddMoneyToGoal = { goalId, amt ->
                        viewModel.addMoneyToGoal(goalId, amt)
                    },
                    onDeleteGoal = { goalId ->
                        viewModel.deleteSavingsGoal(goalId)
                    },
                    modifier = Modifier.padding(innerPadding)
                )
            }

            MainTab.PROFILE -> {
                ProfileScreen(
                    user = userProfile,
                    onUpdateProfile = { name, email, cur, budget ->
                        viewModel.updateUserProfile(name, email, cur, budget)
                    },
                    onUpdateMonthlyBudget = { viewModel.updateMonthlyBudget(it) },
                    onSetDarkMode = { viewModel.setDarkMode(it) },
                    onSetNotifications = { viewModel.setNotificationsEnabled(it) },
                    onExportCsv = { viewModel.getCsvContent() },
                    modifier = Modifier.padding(innerPadding)
                )
            }
        }
    }

    // Add / Edit Transaction Sheet
    if (showAddTransactionSheet) {
        AddEditTransactionSheet(
            initialType = transactionSheetInitialType,
            existingTransaction = transactionToEdit,
            customCategories = customCategories,
            currencySymbol = currency,
            onDismiss = {
                showAddTransactionSheet = false
                transactionToEdit = null
            },
            onSave = { type, amount, title, category, date, method, notes, receiptUri, id ->
                if (type == "EXPENSE") {
                    viewModel.saveExpense(
                        title = title,
                        amount = amount,
                        category = category,
                        date = date,
                        paymentMethod = method,
                        notes = notes,
                        receiptUri = receiptUri,
                        id = id
                    )
                } else {
                    viewModel.saveIncome(
                        title = title,
                        amount = amount,
                        category = category,
                        date = date,
                        paymentMethod = method,
                        notes = notes,
                        id = id
                    )
                }
                showAddTransactionSheet = false
                transactionToEdit = null
            },
            onCreateCustomCategory = { name, icon, isExp ->
                viewModel.addCustomCategory(name, icon, isExp)
            }
        )
    }

    // Set Monthly Budget Dialog from Home
    if (showSetMonthlyBudgetDialog) {
        SetMonthlyBudgetDialog(
            currentBudget = monthlyBudget,
            currencySymbol = currency,
            onDismiss = { showSetMonthlyBudgetDialog = false },
            onSave = { viewModel.updateMonthlyBudget(it) }
        )
    }
}
