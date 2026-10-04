package com.example.ui.components

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.DocumentScanner
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.CustomCategoryEntity
import com.example.data.local.entity.TransactionEntity
import com.example.ui.model.CategoryConstants
import com.example.ui.model.CurrencyHelper
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.IncomeGreen
import com.example.ui.theme.PrimaryIndigo
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddEditTransactionSheet(
    initialType: String = "EXPENSE",
    existingTransaction: TransactionEntity? = null,
    customCategories: List<CustomCategoryEntity> = emptyList(),
    currencySymbol: String = "₹",
    onDismiss: () -> Unit,
    onSave: (
        type: String,
        amount: Double,
        title: String,
        category: String,
        date: Long,
        paymentMethod: String,
        notes: String,
        receiptUri: String?,
        id: Long
    ) -> Unit,
    onCreateCustomCategory: (name: String, icon: String, isExpense: Boolean) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    var selectedType by remember {
        mutableStateOf(existingTransaction?.type ?: initialType)
    }

    var amountText by remember {
        mutableStateOf(
            if (existingTransaction != null) {
                if (existingTransaction.amount % 1.0 == 0.0) {
                    existingTransaction.amount.toLong().toString()
                } else {
                    existingTransaction.amount.toString()
                }
            } else ""
        )
    }

    var title by remember {
        mutableStateOf(existingTransaction?.title ?: "")
    }

    var selectedCategory by remember {
        mutableStateOf(
            existingTransaction?.category ?: if (selectedType == "EXPENSE") "Food" else "Pocket Money"
        )
    }

    var selectedDate by remember {
        mutableLongStateOf(existingTransaction?.date ?: System.currentTimeMillis())
    }

    var selectedPaymentMethod by remember {
        mutableStateOf(existingTransaction?.paymentMethod ?: "UPI")
    }

    var notes by remember {
        mutableStateOf(existingTransaction?.notes ?: "")
    }

    var receiptUri by remember {
        mutableStateOf<String?>(existingTransaction?.receiptUri)
    }

    var showDatePicker by remember { mutableStateOf(false) }
    var showCustomCategoryDialog by remember { mutableStateOf(false) }
    var showReceiptScanDialog by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showSuccessAnimation by remember { mutableStateOf(false) }

    // Media Picker for receipt
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            receiptUri = uri.toString()
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = null,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            // Header with Close and Title
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (existingTransaction != null) {
                        "Edit Transaction"
                    } else if (selectedType == "EXPENSE") {
                        "Add Expense"
                    } else {
                        "Add Income"
                    },
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                IconButton(
                    onClick = { scope.launch { sheetState.hide(); onDismiss() } },
                    modifier = Modifier.testTag("close_sheet_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Type Toggle (Expense / Income) if new transaction
            if (existingTransaction == null) {
                TabRow(
                    selectedTabIndex = if (selectedType == "EXPENSE") 0 else 1,
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                ) {
                    Tab(
                        selected = selectedType == "EXPENSE",
                        onClick = {
                            selectedType = "EXPENSE"
                            if (selectedCategory == "Pocket Money" || selectedCategory == "Salary") {
                                selectedCategory = "Food"
                            }
                        },
                        modifier = Modifier.testTag("tab_expense")
                    ) {
                        Text(
                            text = "Expense",
                            modifier = Modifier.padding(vertical = 10.dp),
                            fontWeight = if (selectedType == "EXPENSE") FontWeight.Bold else FontWeight.Normal,
                            color = if (selectedType == "EXPENSE") ExpenseRed else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Tab(
                        selected = selectedType == "INCOME",
                        onClick = {
                            selectedType = "INCOME"
                            if (selectedCategory == "Food") {
                                selectedCategory = "Pocket Money"
                            }
                        },
                        modifier = Modifier.testTag("tab_income")
                    ) {
                        Text(
                            text = "Income",
                            modifier = Modifier.padding(vertical = 10.dp),
                            fontWeight = if (selectedType == "INCOME") FontWeight.Bold else FontWeight.Normal,
                            color = if (selectedType == "INCOME") IncomeGreen else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Big Amount Input
            Text(
                text = "Amount",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(6.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = currencySymbol,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (selectedType == "EXPENSE") ExpenseRed else IncomeGreen
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    OutlinedTextField(
                        value = amountText,
                        onValueChange = { input ->
                            if (input.all { it.isDigit() || it == '.' } && input.count { it == '.' } <= 1) {
                                amountText = input
                                errorMessage = null
                            }
                        },
                        placeholder = {
                            Text(
                                text = "0",
                                fontSize = 32.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.outline
                            )
                        },
                        textStyle = MaterialTheme.typography.headlineLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        ),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_amount")
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Title / Name Field
            OutlinedTextField(
                value = title,
                onValueChange = { title = it; errorMessage = null },
                label = {
                    Text(if (selectedType == "EXPENSE") "Expense Name (e.g. Lunch)" else "Income Source (e.g. Pocket Money)")
                },
                placeholder = {
                    Text(if (selectedType == "EXPENSE") "College Canteen" else "Monthly Allowance")
                },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_title")
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Category Selection
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Category",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                TextButton(
                    onClick = { showCustomCategoryDialog = true },
                    modifier = Modifier.testTag("add_custom_category_button")
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Add,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "Custom", fontSize = 12.sp)
                }
            }

            // Categories Chips
            val defaultList = if (selectedType == "EXPENSE") {
                CategoryConstants.DEFAULT_EXPENSE_CATEGORIES.map { Pair(it.name, it.icon) }
            } else {
                CategoryConstants.DEFAULT_INCOME_CATEGORIES.map { Pair(it.name, it.icon) }
            }
            val customList = customCategories
                .filter { it.isExpense == (selectedType == "EXPENSE") }
                .map { Pair(it.name, it.icon) }
            val allCats = defaultList + customList

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                allCats.forEach { (catName, icon) ->
                    val isSelected = selectedCategory.equals(catName, ignoreCase = true)
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedCategory = catName },
                        label = { Text("$icon $catName") },
                        shape = RoundedCornerShape(12.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = if (selectedType == "EXPENSE") ExpenseRed.copy(alpha = 0.15f) else IncomeGreen.copy(alpha = 0.15f),
                            selectedLabelColor = if (selectedType == "EXPENSE") ExpenseRed else IncomeGreen
                        ),
                        modifier = Modifier.testTag("chip_category_$catName")
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Date Selection
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Date",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = CurrencyHelper.formatDate(selectedDate),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                OutlinedButton(
                    onClick = { showDatePicker = true },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.testTag("select_date_button")
                ) {
                    Text("Change Date")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Payment Method
            Text(
                text = "Payment Method",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(6.dp))
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CategoryConstants.PAYMENT_METHODS.forEach { method ->
                    val isSelected = selectedPaymentMethod.equals(method, ignoreCase = true)
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedPaymentMethod = method },
                        label = { Text(method) },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("chip_method_$method")
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Notes
            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Notes (Optional)") },
                placeholder = { Text("E.g. Split with Rohan") },
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_notes")
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Receipt & AI Scanner (for expenses)
            if (selectedType == "EXPENSE") {
                Text(
                    text = "Receipt (Optional)",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            photoPickerLauncher.launch(
                                androidx.activity.result.PickVisualMediaRequest(
                                    ActivityResultContracts.PickVisualMedia.ImageOnly
                                )
                            )
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("attach_receipt_button")
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.PhotoCamera,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (receiptUri != null) "Receipt Attached ✓" else "Attach Photo")
                    }

                    Button(
                        onClick = { showReceiptScanDialog = true },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("ai_receipt_scanner_button")
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.DocumentScanner,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("AI Scanner")
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Error validation message
            if (errorMessage != null) {
                Text(
                    text = errorMessage!!,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            // Success Animation View
            AnimatedVisibility(
                visible = showSuccessAnimation,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(IncomeGreen.copy(alpha = 0.15f))
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = IncomeGreen,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (selectedType == "EXPENSE") "Expense added successfully ✓" else "Income added successfully ✓",
                        color = IncomeGreen,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Save Button
            Button(
                onClick = {
                    val amountVal = amountText.toDoubleOrNull()
                    if (amountVal == null || amountText.isBlank()) {
                        errorMessage = "Please enter an amount."
                        return@Button
                    }
                    if (amountVal <= 0.0) {
                        errorMessage = "Amount must be greater than $currencySymbol 0."
                        return@Button
                    }

                    showSuccessAnimation = true
                    scope.launch {
                        delay(400)
                        onSave(
                            selectedType,
                            amountVal,
                            title.ifBlank { selectedCategory },
                            selectedCategory,
                            selectedDate,
                            selectedPaymentMethod,
                            notes,
                            receiptUri,
                            existingTransaction?.id ?: 0L
                        )
                        sheetState.hide()
                        onDismiss()
                    }
                },
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (selectedType == "EXPENSE") ExpenseRed else IncomeGreen
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("save_transaction_button")
            ) {
                Text(
                    text = if (existingTransaction != null) {
                        "Update Transaction"
                    } else if (selectedType == "EXPENSE") {
                        "Save Expense"
                    } else {
                        "Add Income"
                    },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    // Date Picker Dialog
    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = selectedDate)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        selectedDate = datePickerState.selectedDateMillis ?: selectedDate
                        showDatePicker = false
                    }
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    // Custom Category Dialog
    if (showCustomCategoryDialog) {
        var customName by remember { mutableStateOf("") }
        var customIcon by remember { mutableStateOf("🏷️") }
        val sampleIcons = listOf("🍕", "🚲", "🎨", "🎸", "💻", "🎳", "⛺", "🍿", "🏷️", "💳")

        AlertDialog(
            onDismissRequest = { showCustomCategoryDialog = false },
            title = { Text("Create Custom Category") },
            text = {
                Column {
                    OutlinedTextField(
                        value = customName,
                        onValueChange = { customName = it },
                        label = { Text("Category Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Choose Emoji Icon", style = MaterialTheme.typography.labelMedium)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        sampleIcons.take(5).forEach { emoji ->
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(if (customIcon == emoji) PrimaryIndigo.copy(alpha = 0.2f) else Color.Transparent)
                                    .border(
                                        width = if (customIcon == emoji) 2.dp else 1.dp,
                                        color = if (customIcon == emoji) PrimaryIndigo else MaterialTheme.colorScheme.outline,
                                        shape = CircleShape
                                    )
                                    .clickable { customIcon = emoji },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(text = emoji, fontSize = 18.sp)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (customName.isNotBlank()) {
                            onCreateCustomCategory(customName.trim(), customIcon, selectedType == "EXPENSE")
                            selectedCategory = customName.trim()
                            showCustomCategoryDialog = false
                        }
                    }
                ) {
                    Text("Create")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCustomCategoryDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // AI Receipt Scanner Dialog
    if (showReceiptScanDialog) {
        ReceiptScanDialog(
            currencySymbol = currencySymbol,
            onDismiss = { showReceiptScanDialog = false },
            onApply = { parsedMerchant, parsedAmount, parsedCategory, parsedDate, parsedMethod ->
                title = parsedMerchant
                amountText = if (parsedAmount % 1.0 == 0.0) parsedAmount.toLong().toString() else parsedAmount.toString()
                selectedCategory = parsedCategory
                selectedDate = parsedDate
                selectedPaymentMethod = parsedMethod
                showReceiptScanDialog = false
            }
        )
    }
}
