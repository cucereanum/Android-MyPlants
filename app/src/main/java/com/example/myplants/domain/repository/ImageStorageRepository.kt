package com.example.myplants.domain.repository

import android.net.Uri

interface ImageStorageRepository {
    suspend fun persistImage(uri: Uri): Result<String>
}
