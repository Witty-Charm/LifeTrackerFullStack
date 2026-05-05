package com.lifetracker.mobile.data.auth

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import timber.log.Timber

/**
 * Wraps Credential Manager + Google ID into a single suspending call.
 * Returns the raw id_token that the backend can validate against
 * GoogleWebClientId.
 */
class GoogleSignInClient(
    private val context: Context,
    private val webClientId: String,
) {
    private val credentialManager = CredentialManager.create(context)

    suspend fun fetchIdToken(): Result<String> {
        if (webClientId.isBlank()) {
            return Result.failure(IllegalStateException("Google Web Client ID is not configured."))
        }
        return runCatching { request(useExplicitSignIn = false) }
            .recoverCatching { error ->
                if (error is NoCredentialException) {
                    Timber.w(error, "GetGoogleIdOption returned NoCredentialException; falling back to GetSignInWithGoogleOption.")
                    request(useExplicitSignIn = true)
                } else {
                    throw error
                }
            }
            .onFailure { error ->
                if (error is GetCredentialException) {
                    Timber.w(error, "Credential Manager rejected sign-in.")
                } else {
                    Timber.w(error, "Sign-in flow failed.")
                }
            }
    }

    private suspend fun request(useExplicitSignIn: Boolean): String {
        val option =
            if (useExplicitSignIn) {
                GetSignInWithGoogleOption.Builder(webClientId).build()
            } else {
                GetGoogleIdOption.Builder()
                    .setServerClientId(webClientId)
                    .setFilterByAuthorizedAccounts(false)
                    .setAutoSelectEnabled(false)
                    .build()
            }
        val request = GetCredentialRequest.Builder().addCredentialOption(option).build()
        val response = credentialManager.getCredential(context, request)
        val credential = response.credential
        if (credential !is CustomCredential ||
            credential.type != GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
        ) {
            error("Unexpected credential type: ${credential::class.java.name}")
        }
        return try {
            GoogleIdTokenCredential.createFrom(credential.data).idToken
        } catch (parse: GoogleIdTokenParsingException) {
            throw IllegalStateException("Failed to parse Google ID Token credential.", parse)
        }
    }
}
