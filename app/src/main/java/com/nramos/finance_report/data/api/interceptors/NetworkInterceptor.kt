package com.nramos.finance_report.data.api.interceptors
import okhttp3.Interceptor
import okhttp3.Response

class NetworkInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val response = chain.proceed(request)

        // Aquí puedes manejar errores globales como 401, 403, etc.
        when (response.code) {
            401 -> {
                // Token expirado, redirigir a login
            }
            403 -> {
                // Acceso denegado
            }
        }

        return response
    }
}