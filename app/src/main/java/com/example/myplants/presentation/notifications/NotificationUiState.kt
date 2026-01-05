package com.example.myplants.presentation.notifications

import androidx.compose.runtime.Immutable
import com.example.myplants.data.NotificationEntity

@Immutable
data class NotificationUiState(
    val notifications: List<NotificationEntity> = emptyList(),
    val groupedNotifications: Map<String, List<NotificationEntity>> = emptyMap(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
)
