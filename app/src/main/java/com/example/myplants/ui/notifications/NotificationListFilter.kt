package com.example.myplants.ui.notifications

import com.example.myplants.ui.util.FilterType


enum class NotificationListFilter(override val displayName: String) : FilterType {
    ALL_NOTIFICATIONS("All Notifications"),
    FORGOT_TO_WATER("Forgot to Water"),
    HISTORY("History")
}