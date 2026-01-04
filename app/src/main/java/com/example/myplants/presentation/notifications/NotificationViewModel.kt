package com.example.myplants.presentation.notifications


import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myplants.data.NotificationEntity
import com.example.myplants.domain.repository.NotificationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

@RequiresApi(Build.VERSION_CODES.O)
@HiltViewModel
class NotificationViewModel @Inject constructor(
    private val repository: NotificationRepository
) : ViewModel() {
    
    val uiState: StateFlow<NotificationUiState> = repository
        .getAllNotifications()
        .map { allNotifications ->
            val today = LocalDate.now()
            val yesterday = today.minusDays(1)

            val grouped = allNotifications.groupBy { notification ->
                val date = Instant
                    .ofEpochMilli(notification.timestamp)
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate()

                when (date) {
                    today -> "Today"
                    yesterday -> "Yesterday"
                    else -> date.toString()
                }
            }

            NotificationUiState(
                notifications = allNotifications,
                groupedNotifications = grouped,
                isLoading = false,
                errorMessage = null,
            )
        }
        .onStart {
            emit(NotificationUiState(isLoading = true))
        }
        .catch { throwable ->
            Log.e("NotificationViewModel", "Failed to load notifications", throwable)
            emit(
                NotificationUiState(
                    isLoading = false,
                    errorMessage = throwable.message,
                )
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = NotificationUiState(isLoading = true),
        )

    fun markNotificationsAsRead(ids: List<Int>) {
        viewModelScope.launch {
            repository.markAsReadByIds(ids)
        }
    }
}