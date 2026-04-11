package com.nramos.finance_report.data.auth

import android.content.Context
import android.content.Intent
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.nramos.finance_report.BuildConfig
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

@Singleton
class GoogleSignInManager @Inject constructor(
    private val context: Context
) {

    private val googleSignInClient: GoogleSignInClient by lazy {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(BuildConfig.GOOGLE_WEB_CLIENT_ID)
            .requestEmail()
            .build()

        GoogleSignIn.getClient(context, gso)
    }

    fun getSignInIntent(): Intent {
        return googleSignInClient.signInIntent
    }

    suspend fun handleSignInResult(data: Intent?): Result<GoogleSignInResult> = suspendCancellableCoroutine { continuation ->
        try {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            val account = task.getResult(ApiException::class.java)

            if (account != null) {
                val idToken = account.idToken
                if (!idToken.isNullOrEmpty()) {
                    continuation.resume(Result.success(GoogleSignInResult(
                        idToken = idToken,
                        email = account.email ?: "",
                        name = account.displayName ?: "",
                        givenName = account.givenName,
                        familyName = account.familyName,
                        photoUrl = account.photoUrl?.toString()
                    )))
                } else {
                    continuation.resumeWithException(Exception("No se pudo obtener el token ID de Google"))
                }
            } else {
                continuation.resumeWithException(Exception("Cuenta de Google no válida"))
            }
        } catch (e: ApiException) {
            continuation.resumeWithException(e)
        }
    }

    suspend fun signOut() {
        googleSignInClient.signOut().await()
    }

    fun isSignedIn(): Boolean {
        return GoogleSignIn.getLastSignedInAccount(context) != null
    }
}

data class GoogleSignInResult(
    val idToken: String,
    val email: String,
    val name: String,
    val givenName: String? = null,
    val familyName: String? = null,
    val photoUrl: String? = null
)

suspend fun <T> com.google.android.gms.tasks.Task<T>.await(): T {
    return suspendCancellableCoroutine { continuation ->
        addOnSuccessListener { result ->
            continuation.resume(result)
        }
        addOnFailureListener { exception ->
            continuation.resumeWithException(exception)
        }
    }
}