package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val type: String, // "EXPENSE" or "INCOME"
    val amount: Double,
    val title: String,
    val category: String,
    val date: Long,
    val paymentMethod: String = "UPI",
    val notes: String = "",
    val receiptUri: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)
