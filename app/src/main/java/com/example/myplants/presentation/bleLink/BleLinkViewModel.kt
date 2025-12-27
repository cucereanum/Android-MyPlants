package com.example.myplants.presentation.bleLink

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myplants.data.ble.BleDevice
import com.example.myplants.domain.repository.BleDatabaseRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BleLinkViewModel @Inject constructor(
    private val bleDatabaseRepository: BleDatabaseRepository,
) : ViewModel() {

    fun linkDeviceToPlant(plantId: Int, device: BleDevice) {
        viewModelScope.launch {
            bleDatabaseRepository.linkDeviceToPlant(plantId = plantId, device = device)
        }
    }
}
