package com.example.myplants.presentation.plantList

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myplants.data.DayOfWeek
import com.example.myplants.data.Plant
import com.example.myplants.domain.repository.NotificationRepository
import com.example.myplants.domain.repository.PlantRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

@HiltViewModel
class PlantListViewModel @Inject constructor(
    private val repository: PlantRepository,
    private val notificationRepository: NotificationRepository
) : ViewModel() {

    companion object {
        private const val PAGE_SIZE = 10
    }

    var filterList by mutableStateOf(PlantListFilter.entries)
        private set

    private val _uiState = MutableStateFlow(PlantListUiState())
    val uiState: StateFlow<PlantListUiState> = _uiState

    private var allFilteredPlants: List<Plant> = emptyList()
    private var currentPage = 0

    init {
        loadInitialPlants()
    }

    val hasUnreadNotifications = notificationRepository
        .hasUnreadNotificationsFlow()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false
        )

    private fun loadInitialPlants() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            repository.getPlants().collect { allPlants ->
                val filtered = filterPlants(allPlants, _uiState.value.selectedFilterType)
                allFilteredPlants = filtered
                currentPage = 0

                val initialBatch = filtered.take(PAGE_SIZE)
                _uiState.update {
                    it.copy(
                        plants = initialBatch,
                        isLoading = false,
                        hasMoreToLoad = filtered.size > PAGE_SIZE
                    )
                }
            }
        }
    }

    fun selectFilter(type: PlantListFilter) {
        if (_uiState.value.selectedFilterType == type) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, selectedFilterType = type) }

            repository.getPlants().collect { allPlants ->
                val filtered = filterPlants(allPlants, type)
                allFilteredPlants = filtered
                currentPage = 0

                val initialBatch = filtered.take(PAGE_SIZE)
                _uiState.update {
                    it.copy(
                        plants = initialBatch,
                        isLoading = false,
                        hasMoreToLoad = filtered.size > PAGE_SIZE,
                        selectedFilterType = type
                    )
                }
            }
        }
    }

    fun loadNextPage() {
        if (_uiState.value.isLoadingMore || !_uiState.value.hasMoreToLoad) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingMore = true) }

            currentPage++
            val startIndex = currentPage * PAGE_SIZE
            val endIndex = minOf(startIndex + PAGE_SIZE, allFilteredPlants.size)

            if (startIndex < allFilteredPlants.size) {
                val nextBatch = allFilteredPlants.subList(startIndex, endIndex)
                val updatedList = _uiState.value.plants + nextBatch

                _uiState.update {
                    it.copy(
                        plants = updatedList,
                        isLoadingMore = false,
                        hasMoreToLoad = endIndex < allFilteredPlants.size
                    )
                }
            } else {
                _uiState.update {
                    it.copy(
                        isLoadingMore = false,
                        hasMoreToLoad = false
                    )
                }
            }
        }
    }

    private fun filterPlants(allPlants: List<Plant>, filterType: PlantListFilter): List<Plant> {
        val today = DayOfWeek.today()
        val calendar = Calendar.getInstance()
        val currentTimeOfDayMillis =
            calendar.get(Calendar.HOUR_OF_DAY) * 60L * 60L * 1000L +
                    calendar.get(Calendar.MINUTE) * 60L * 1000L +
                    calendar.get(Calendar.SECOND) * 1000L +
                    calendar.get(Calendar.MILLISECOND)

        return when (filterType) {
            PlantListFilter.FORGOT_TO_WATER -> allPlants.filter { plant ->
                !plant.isWatered && plant.selectedDays.any { day ->
                    day.ordinal < today.ordinal ||
                            (day == today && plant.time < currentTimeOfDayMillis)
                }
            }

            PlantListFilter.UPCOMING -> allPlants.filter { plant ->
                !plant.isWatered &&
                        !plant.selectedDays.any { day ->
                            day.ordinal < today.ordinal ||
                                    (day == today && plant.time < currentTimeOfDayMillis)
                        } &&
                        plant.selectedDays.any { day ->
                            day.ordinal > today.ordinal ||
                                    (day == today && plant.time > currentTimeOfDayMillis)
                        }
            }

            PlantListFilter.HISTORY -> allPlants.filter { plant ->
                plant.isWatered
            }
        }
    }
}
