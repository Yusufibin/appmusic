package com.company.product

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "presets")
data class Preset(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val volMusic: Float,
    val volChat: Float,
    val volSystem: Float,
    val appTarget: String? = null
)