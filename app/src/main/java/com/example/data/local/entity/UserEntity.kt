package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey
    val id: Int = 1,
    val name: String = "Student",
    val email: String = "student@college.edu",
    val currency: String = "₹",
    val monthlyBudget: Double = 10000.0,
    val darkMode: Boolean? = null, // null = system default, true = dark, false = light
    val notificationsEnabled: Boolean = true,
    val onboardingCompleted: Boolean = false,
    val isProUser: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
