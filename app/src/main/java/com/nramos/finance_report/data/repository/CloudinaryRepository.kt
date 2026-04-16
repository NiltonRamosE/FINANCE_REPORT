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

    fun initCloudinary(context: Context, cloudName: String, apiKey: String, apiSecret: String) {
        if (!isInitialized) {
            try {
                val config = HashMap<String, String>().apply {
                    put("cloud_name", cloudName)
                    put("api_key", apiKey)
                    put("api_secret", apiSecret)
                }
                MediaManager.init(context, config)
                isInitialized = true
            } catch (e: Exception) {
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
                    }

                    override fun onProgress(requestId: String, bytes: Long, totalBytes: Long) {
                        val progress = if (totalBytes > 0) (bytes * 100 / totalBytes).toInt() else 0
                    }

                    override fun onSuccess(requestId: String, resultData: Map<*, *>) {
                        val url = resultData["secure_url"] as? String
                        if (url != null) {
                            trySend(url)
                            close()
                        } else {
                            close(Exception("No se obtuvo URL de Cloudinary"))
                        }
                    }

                    override fun onError(requestId: String, error: ErrorInfo) {
                        close(Exception(error.description))
                    }

                    override fun onReschedule(requestId: String, error: ErrorInfo) {
                    }
                })
                .dispatch()

            awaitClose {
            }
        } catch (e: Exception) {
            close(e)
        }
    }
}