package com.example.myplants.ui.notifications


import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myplants.data.NotificationEntity
import com.example.myplants.domain.repository.NotificationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
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

    private val _items = MutableStateFlow<List<NotificationEntity>>(emptyList())

    val items: StateFlow<List<NotificationEntity>> = _items.asStateFlow()

    private val _groupedItems = MutableStateFlow<Map<String, List<NotificationEntity>>>(emptyMap())
    val groupedItems: StateFlow<Map<String, List<NotificationEntity>>> = _groupedItems.asStateFlow()

    var isLoading by mutableStateOf(false)
        private set


    fun getNotifications(showLoading: Boolean = true) {
        viewModelScope.launch {
            if (showLoading) {
                isLoading = true
            }
            try {
                val allNotifications = repository.getAllNotifications().first()

                val today = LocalDate.now()
                val yesterday = today.minusDays(1)

                val grouped = allNotifications.groupBy { notif ->
                    val date = Instant.ofEpochMilli(notif.timestamp)
                        .atZone(ZoneId.systemDefault()).toLocalDate()

                    when (date) {
                        today -> "Today"
                        yesterday -> "Yesterday"
                        else -> date.toString()
                    }
                }
                _items.value = allNotifications
                _groupedItems.value = grouped
            } catch (e: Exception) {
                Log.e("Notifications", "Error: ${e.message}")
            } finally {
                isLoading = false
            }
        }
    }

    fun markNotificationsAsRead(ids: List<Int>) {
        viewModelScope.launch {
            repository.markAsReadByIds(ids)
            delay(2000)
            getNotifications(false)
        }
    }
}