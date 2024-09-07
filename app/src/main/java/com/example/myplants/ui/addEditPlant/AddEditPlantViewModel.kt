package com.example.myplants.ui.addEditPlant

import android.content.Context
import android.net.Uri
import android.os.Environment
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date

class AddEditPlantViewModel : ViewModel() {

    var imageUri by mutableStateOf<Uri?>(null)
        private set

    var showDialog by mutableStateOf<Boolean>(false)
        private set

    fun updateImageUri(uri: Uri?) {
        imageUri = uri;
    }

    fun updateShowDialog(value: Boolean) {
        showDialog = value
    }


}