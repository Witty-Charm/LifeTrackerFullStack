package com.lifetracker.mobile.data.auth

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
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
 *
 * Uses a three-step fallback ladder so we degrade gracefully across the
 * known-flaky Credential Manager states:
 *   1. GetGoogleIdOption(filterByAuthorizedAccounts = true)  — silent / one-tap
 *      for accounts that already authorized the app.
 *   2. GetGoogleIdOption(filterByAuthorizedAccounts = false) — bottom-sheet
 *      account picker over every Google account on the device.
 *   3. GetSignInWithGoogleOption                              — explicit
 *      "Sign in with Google" sheet; the only flow that consistently shows
 *      the account chooser when authorized accounts are stale.
 *
 * NoCredentialException, GetCredentialCancellationException and any other
 * GetCredentialException are treated as soft failures and trigger the next
 * step. The last failure is surfaced so the UI can show a meaningful error.
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

        val attempts =
            listOf(
                Attempt("GetGoogleIdOption(authorized=true)") {
                    GetGoogleIdOption.Builder()
                        .setServerClientId(webClientId)
                        .setFilterByAuthorizedAccounts(true)
                        .setAutoSelectEnabled(true)
                        .build()
                },
                Attempt("GetGoogleIdOption(authorized=false)") {
                    GetGoogleIdOption.Builder()
                        .setServerClientId(webClientId)
                        .setFilterByAuthorizedAccounts(false)
                        .setAutoSelectEnabled(false)
                        .build()
                },
                Attempt("GetSignInWithGoogleOption") {
                    GetSignInWithGoogleOption.Builder(webClientId).build()
                },
            )

        var lastError: Throwable? = null
        for (attempt in attempts) {
            try {
                val response = credentialManager.getCredential(context, attempt.buildRequest())
                return Result.success(extractIdToken(response))
            } catch (error: GetCredentialException) {
                Timber.w(
                    error,
                    "Credential Manager step '${attempt.label}' failed: type=${error.type} message=${error.message}",
                )
                lastError = error
                if (!shouldFallback(error)) {
                    return Result.failure(translateError(error))
                }
            } catch (error: Throwable) {
                Timber.w(error, "Credential Manager step '${attempt.label}' threw unexpectedly.")
                return Result.failure(error)
            }
        }
        return Result.failure(
            translateError(
                lastError ?: IllegalStateException("Sign-in failed: no credential available."),
            ),
        )
    }

    private fun translateError(error: Throwable): Throwable {
        val msg = error.message.orEmpty()
        // GMS surfaces "[16] account reauth failed" for stale device-account
        // state. Wrap it with a hint so the UI can tell the user what to do.
        if (msg.contains("reauth", ignoreCase = true) || msg.contains("[16]")) {
            return IllegalStateException(
                "Google account on this device needs to be re-authenticated. " +
                    "Open Settings → Accounts → Google, sync the account or remove and add it again, then try sign-in.",
                error,
            )
        }
        return error
    }

    private fun shouldFallback(error: GetCredentialException): Boolean {
        // NoCredentialException always gives the next option a chance.
        if (error is NoCredentialException) return true
        // GMS reports user-cancellation, reauth failures, etc. via the `type` field.
        // We retry on anything that isn't an interrupt (the user explicitly
        // cancelled the picker on screen). USER_CANCELED is preserved up to the
        // caller so the UI can show "cancelled" instead of cycling pickers.
        val type = error.type
        if (type.endsWith(".TYPE_USER_CANCELED")) return false
        return true
    }

    private fun extractIdToken(response: GetCredentialResponse): String {
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

    private class Attempt(
        val label: String,
        val buildOption: () -> androidx.credentials.CredentialOption,
    ) {
        fun buildRequest(): GetCredentialRequest =
            GetCredentialRequest.Builder()
                .addCredentialOption(buildOption())
                .build()
    }
}
