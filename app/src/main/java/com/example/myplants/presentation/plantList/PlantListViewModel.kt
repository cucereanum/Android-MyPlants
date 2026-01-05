package com.example.myplants.presentation.plantList

import android.util.Log
import androidx.compose.runtime.Immutable
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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import java.util.Calendar
import javax.inject.Inject

@HiltViewModel
class PlantListViewModel @Inject constructor(
    private val repository: PlantRepository,
    private val notificationRepository: NotificationRepository
) : ViewModel() {

    var filterList by mutableStateOf(PlantListFilter.entries)
        private set

    private val selectedFilterTypeStateFlow = MutableStateFlow(PlantListFilter.UPCOMING)

    private val filteredPlantsFlow: Flow<List<Plant>> =
        combine(
            repository.getPlants(),
            selectedFilterTypeStateFlow,
        ) { allPlants, selectedFilterType ->
            val today = DayOfWeek.today()
            val calendar = Calendar.getInstance()
            val currentTimeOfDayMillis =
                calendar.get(Calendar.HOUR_OF_DAY) * 60L * 60L * 1000L +
                        calendar.get(Calendar.MINUTE) * 60L * 1000L +
                        calendar.get(Calendar.SECOND) * 1000L +
                        calendar.get(Calendar.MILLISECOND)

            when (selectedFilterType) {
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


    val uiState: StateFlow<PlantListUiState> =
        combine(
            selectedFilterTypeStateFlow,
            filteredPlantsFlow,
        ) { selectedFilterType, filteredPlants ->
            PlantListUiState(
                plants = filteredPlants,
                selectedFilterType = selectedFilterType,
                isLoading = false,
                errorMessage = null,
            )
        }
            .onStart {
                emit(
                    PlantListUiState(
                        selectedFilterType = selectedFilterTypeStateFlow.value,
                        isLoading = true,
                    )
                )
            }
            .catch { throwable ->
                Log.e("PlantListViewModel", "Failed to load plants", throwable)
                emit(
                    PlantListUiState(
                        selectedFilterType = selectedFilterTypeStateFlow.value,
                        isLoading = false,
                        errorMessage = throwable.message,
                    )
                )
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = PlantListUiState(
                    selectedFilterType = selectedFilterTypeStateFlow.value,
                    isLoading = true,
                ),
            )

    val hasUnreadNotifications = notificationRepository
        .hasUnreadNotificationsFlow()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false
        )

    fun selectFilter(type: PlantListFilter) {
        selectedFilterTypeStateFlow.value = type
    }
}