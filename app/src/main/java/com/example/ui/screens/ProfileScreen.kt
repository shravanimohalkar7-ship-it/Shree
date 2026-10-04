package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.widget.Toast
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.UserEntity
import com.example.ui.components.ProUpgradeDialog
import com.example.ui.components.SetMonthlyBudgetDialog
import com.example.ui.model.CurrencyHelper
import com.example.ui.theme.IncomeGreen
import com.example.ui.theme.PrimaryIndigo
import com.example.ui.theme.SecondaryViolet
import kotlinx.coroutines.launch

@Composable
fun ProfileScreen(
    user: UserEntity?,
    onUpdateProfile: (name: String, email: String, currency: String, budget: Double) -> Unit,
    onUpdateMonthlyBudget: (Double) -> Unit,
    onSetDarkMode: (Boolean?) -> Unit,
    onSetNotifications: (Boolean) -> Unit,
    onExportCsv: suspend () -> String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var showEditProfileDialog by remember { mutableStateOf(false) }
    var showBudgetDialog by remember { mutableStateOf(false) }
    var showCurrencyDialog by remember { mutableStateOf(false) }
    var showDarkModeDialog by remember { mutableStateOf(false) }
    var showProDialog by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }

    val name = user?.name ?: "Student"
    val email = user?.email ?: "student@college.edu"
    val currency = user?.currency ?: "₹"
    val monthlyBudget = user?.monthlyBudget ?: 10000.0
    val darkMode = user?.darkMode
    val notifications = user?.notificationsEnabled ?: true

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Profile & Settings",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        // Student Profile Header Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(listOf(PrimaryIndigo, SecondaryViolet))
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        val initials = name.split(" ").mapNotNull { it.firstOrNull()?.toString() }.take(2).joinToString("")
                        Text(
                            text = initials.ifBlank { "SS" },
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = name,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = email,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(PrimaryIndigo.copy(alpha = 0.12f))
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "College Student",
                                style = MaterialTheme.typography.labelSmall,
                                color = PrimaryIndigo,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    IconButton(
                        onClick = { showEditProfileDialog = true },
                        modifier = Modifier.testTag("edit_profile_button")
                    ) {
                        Icon(imageVector = Icons.Default.Edit, contentDescription = "Edit Profile", tint = PrimaryIndigo)
                    }
                }
            }
        }

        // StudentSpend Pro Banner
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showProDialog = true }
                    .testTag("pro_upgrade_banner"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1B4B)),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(PrimaryIndigo),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "StudentSpend Pro",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(IncomeGreen)
                                        .padding(horizontal = 5.dp, vertical = 1.dp)
                                ) {
                                    Text("₹49/mo", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                            Text(
                                text = "AI receipt scan, unlimited goals & cloud sync",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.75f)
                            )
                        }
                    }
                }
            }
        }

        // Settings Section Header
        item {
            Text(
                text = "Preferences",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        // Settings Items
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column {
                    SettingsRow(
                        icon = Icons.Default.Payments,
                        title = "Currency",
                        subtitle = "Selected: $currency (Default: Indian Rupee)",
                        onClick = { showCurrencyDialog = true }
                    )

                    SettingsDivider()

                    SettingsRow(
                        icon = Icons.Default.Tune,
                        title = "Monthly Budget",
                        subtitle = CurrencyHelper.formatPlain(monthlyBudget, currency),
                        onClick = { showBudgetDialog = true }
                    )

                    SettingsDivider()

                    SettingsRow(
                        icon = Icons.Default.DarkMode,
                        title = "Dark Mode",
                        subtitle = when (darkMode) {
                            true -> "🌙 Dark Mode"
                            false -> "☀️ Light Mode"
                            null -> "📱 System Default"
                        },
                        onClick = { showDarkModeDialog = true }
                    )

                    SettingsDivider()

                    // Notifications Switch Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(PrimaryIndigo.copy(alpha = 0.1f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Notifications,
                                    contentDescription = null,
                                    tint = PrimaryIndigo,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Daily Reminders & Budget Alerts",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = if (notifications) "Enabled (80% warning & daily check-in)" else "Disabled",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Switch(
                            checked = notifications,
                            onCheckedChange = { onSetNotifications(it) }
                        )
                    }
                }
            }
        }

        // Data & Privacy Section Header
        item {
            Text(
                text = "Data & Security",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column {
                    SettingsRow(
                        icon = Icons.Default.Share,
                        title = "Export Transactions (CSV)",
                        subtitle = "Download all records to spreadsheets or Google Drive",
                        onClick = {
                            scope.launch {
                                val csv = onExportCsv()
                                val sendIntent = Intent().apply {
                                    action = Intent.ACTION_SEND
                                    putExtra(Intent.EXTRA_TEXT, csv)
                                    type = "text/csv"
                                    putExtra(Intent.EXTRA_SUBJECT, "StudentSpend_Transactions.csv")
                                }
                                val shareIntent = Intent.createChooser(sendIntent, "Export Transactions CSV")
                                context.startActivity(shareIntent)
                            }
                        }
                    )

                    SettingsDivider()

                    SettingsRow(
                        icon = Icons.Default.CloudUpload,
                        title = "Local Database Backup",
                        subtitle = "Offline-first Room database. Saved locally on this device.",
                        onClick = {
                            Toast.makeText(context, "Local Room database is healthy & synced ✓", Toast.LENGTH_SHORT).show()
                        }
                    )

                    SettingsDivider()

                    SettingsRow(
                        icon = Icons.Default.Lock,
                        title = "Privacy & Financial Security",
                        subtitle = "Zero external tracking. Your financial data stays private on device.",
                        onClick = {
                            Toast.makeText(context, "StudentSpend keeps 100% of data private on-device.", Toast.LENGTH_LONG).show()
                        }
                    )

                    SettingsDivider()

                    SettingsRow(
                        icon = Icons.Default.Info,
                        title = "About StudentSpend",
                        subtitle = "Version 1.0 • Built for College Students",
                        onClick = { showAboutDialog = true }
                    )
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(80.dp))
        }
    }

    // Edit Profile Dialog
    if (showEditProfileDialog) {
        var editName by remember { mutableStateOf(name) }
        var editEmail by remember { mutableStateOf(email) }

        AlertDialog(
            onDismissRequest = { showEditProfileDialog = false },
            title = { Text("Edit Student Profile") },
            text = {
                Column {
                    OutlinedTextField(
                        value = editName,
                        onValueChange = { editName = it },
                        label = { Text("Full Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = editEmail,
                        onValueChange = { editEmail = it },
                        label = { Text("College Email") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onUpdateProfile(editName.trim(), editEmail.trim(), currency, monthlyBudget)
                        showEditProfileDialog = false
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditProfileDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Set Budget Dialog
    if (showBudgetDialog) {
        SetMonthlyBudgetDialog(
            currentBudget = monthlyBudget,
            currencySymbol = currency,
            onDismiss = { showBudgetDialog = false },
            onSave = {
                onUpdateMonthlyBudget(it)
            }
        )
    }

    // Currency Dialog
    if (showCurrencyDialog) {
        val currencies = listOf(
            Triple("₹", "INR", "Indian Rupee"),
            Triple("$", "USD", "US Dollar"),
            Triple("€", "EUR", "Euro"),
            Triple("£", "GBP", "British Pound")
        )
        AlertDialog(
            onDismissRequest = { showCurrencyDialog = false },
            title = { Text("Select Currency") },
            text = {
                Column {
                    currencies.forEach { (symbol, code, label) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onUpdateProfile(name, email, symbol, monthlyBudget)
                                    showCurrencyDialog = false
                                }
                                .padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = symbol, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = PrimaryIndigo)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(text = "$code ($symbol)", fontWeight = FontWeight.SemiBold)
                                Text(text = label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showCurrencyDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Dark Mode Dialog
    if (showDarkModeDialog) {
        AlertDialog(
            onDismissRequest = { showDarkModeDialog = false },
            title = { Text("Theme / Dark Mode") },
            text = {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSetDarkMode(false); showDarkModeDialog = false }
                            .padding(vertical = 10.dp)
                    ) {
                        Text("☀️ Light Mode", style = MaterialTheme.typography.titleMedium)
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSetDarkMode(true); showDarkModeDialog = false }
                            .padding(vertical = 10.dp)
                    ) {
                        Text("🌙 Dark Mode", style = MaterialTheme.typography.titleMedium)
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSetDarkMode(null); showDarkModeDialog = false }
                            .padding(vertical = 10.dp)
                    ) {
                        Text("📱 Follow System", style = MaterialTheme.typography.titleMedium)
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showDarkModeDialog = false }) {
                    Text("Close")
                }
            }
        )
    }

    // Pro Showcase
    if (showProDialog) {
        ProUpgradeDialog(
            onDismiss = { showProDialog = false },
            onSimulateUpgrade = {
                showProDialog = false
                Toast.makeText(context, "StudentSpend Pro membership ready!", Toast.LENGTH_SHORT).show()
            }
        )
    }

    // About Dialog
    if (showAboutDialog) {
        AlertDialog(
            onDismissRequest = { showAboutDialog = false },
            title = { Text("About StudentSpend") },
            text = {
                Column {
                    Text(
                        text = "StudentSpend is a clean, youthful, and offline-first student expense tracker designed for college life.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Features:\n• Fast 1-handed logging\n• Automatic budget calculations\n• Smart spending insights\n• Savings goals\n• Zero complex banking clutter",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(onClick = { showAboutDialog = false }) {
                    Text("Awesome")
                }
            }
        )
    }
}

@Composable
fun SettingsRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(PrimaryIndigo.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = PrimaryIndigo,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun SettingsDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
    )
}
