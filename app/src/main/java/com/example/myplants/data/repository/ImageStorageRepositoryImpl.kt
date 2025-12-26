package com.example.myplants.data.repository

import android.content.Context
import android.net.Uri
import com.example.myplants.domain.repository.ImageStorageRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ImageStorageRepositoryImpl @Inject constructor(
    @ApplicationContext private val appContext: Context,
) : ImageStorageRepository {

    override suspend fun persistImage(uri: Uri): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val inputStream = appContext.contentResolver.openInputStream(uri)
                ?: throw IllegalStateException("Unable to open input stream for uri=$uri")

            val outputFile = File(
                appContext.filesDir,
                "saved_plant_image_${System.currentTimeMillis()}.jpg"
            )

            inputStream.use { input ->
                outputFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }

            outputFile.absolutePath
        }
    }
}
