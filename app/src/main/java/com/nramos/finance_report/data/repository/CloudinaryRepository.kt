package com.nramos.finance_report.data.repository

import android.content.Context
import android.net.Uri
import android.util.Log
import com.cloudinary.android.MediaManager
import com.cloudinary.android.callback.ErrorInfo
import com.cloudinary.android.callback.UploadCallback
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import java.util.HashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CloudinaryRepository @Inject constructor() {

    private var isInitialized = false

    fun initCloudinary(context: Context, cloudName: String) {
        if (!isInitialized) {
            try {
                val config = HashMap<String, String>()
                config["cloud_name"] = cloudName
                MediaManager.init(context, config)
                isInitialized = true
                Log.d("Cloudinary", "Cloudinary initialized successfully")
            } catch (e: Exception) {
                Log.e("Cloudinary", "Error initializing: ${e.message}")
            }
        }
    }

    suspend fun uploadImage(imageUri: Uri, uploadPreset: String): Flow<String> = callbackFlow {
        try {
            if (!isInitialized) {
                close(Exception("Cloudinary no está inicializado"))
                return@callbackFlow
            }

            val requestId = MediaManager.get().upload(imageUri)
                .option("upload_preset", uploadPreset)
                .callback(object : UploadCallback {
                    override fun onStart(requestId: String) {
                        Log.d("Cloudinary", "Upload started: $requestId")
                    }

                    override fun onProgress(requestId: String, bytes: Long, totalBytes: Long) {
                        val progress = if (totalBytes > 0) (bytes * 100 / totalBytes).toInt() else 0
                        Log.d("Cloudinary", "Upload progress: $progress%")
                    }

                    override fun onSuccess(requestId: String, resultData: Map<*, *>) {
                        Log.d("Cloudinary", "Upload success")
                        val url = resultData["secure_url"] as? String
                        if (url != null) {
                            trySend(url)
                            close()
                        } else {
                            close(Exception("No se obtuvo URL de Cloudinary"))
                        }
                    }

                    override fun onError(requestId: String, error: ErrorInfo) {
                        Log.e("Cloudinary", "Upload error: ${error.description}")
                        close(Exception(error.description))
                    }

                    override fun onReschedule(requestId: String, error: ErrorInfo) {
                        Log.d("Cloudinary", "Upload rescheduled: ${error.description}")
                    }
                })
                .dispatch()

            awaitClose {
                Log.d("Cloudinary", "Upload flow closed")
            }
        } catch (e: Exception) {
            Log.e("Cloudinary", "Upload exception: ${e.message}")
            close(e)
        }
    }
}