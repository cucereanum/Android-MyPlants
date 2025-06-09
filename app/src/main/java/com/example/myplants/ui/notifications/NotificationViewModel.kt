package com.example.myplants.ui.notifications


import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myplants.data.NotificationEntity
import com.example.myplants.domain.repository.NotificationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NotificationViewModel @Inject constructor(
    private val repository: NotificationRepository
) : ViewModel() {

    var filterList by mutableStateOf(NotificationListFilter.entries)
        private set

    var selectedFilterType by mutableStateOf(NotificationListFilter.ALL_NOTIFICATIONS)
        private set

    private val _items = MutableStateFlow<List<NotificationEntity>>(emptyList())

    val items: StateFlow<List<NotificationEntity>> = _items.asStateFlow()

    var isLoading by mutableStateOf(false)
        private set


    fun selectFilter(type: NotificationListFilter) {
        selectedFilterType = type
        filterNotifications()
    }


//    suspend fun getPlants() {
//        isLoading = true
//        try {
//            val itemsList = repository.getPlants().first()
//            _items.value = itemsList
//            filterPlants()
//        } catch (e: Exception) {
//            e.message?.let { Log.e("Get Plant List Error", it) }
//        } finally {
//            isLoading = false
//        }
//    }

    private fun filterNotifications() {
        viewModelScope.launch {
            val allPlants = repository.getAllNotifications().first()
//            val currentTime = System.currentTimeMillis()
//
//            val filteredList = when (selectedFilterType) {
//                NotificationListFilter.ALL_NOTIFICAITONS -> allPlants
//
//                NotificationListFilter.FORGOT_TO_WATER -> allPlants
//
//                NotificationListFilter.HISTORY -> allPlants
//
//            }

            _items.value = allPlants
        }
    }

}