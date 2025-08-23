package com.example.myplants.presentation.deeplink

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import javax.inject.Inject

@HiltViewModel
class DeepLinkViewModel @Inject constructor() : ViewModel() {
    private val _openPlant = MutableSharedFlow<Int>(extraBufferCapacity = 1)
    val openPlant: Flow<Int> = _openPlant

    fun open(plantId: Int) {
        _openPlant.tryEmit(plantId)
    }
}