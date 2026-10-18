package com.saney.renaultdocs

import android.app.Activity
import android.content.Context
import android.os.CancellationSignal
import android.os.Handler
import android.os.Looper
import androidx.core.content.ContextCompat
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CredentialManagerCallback
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.exceptions.ClearCredentialException
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.GetCredentialProviderConfigurationException
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential

/**
 * 0.6.5 (#133, step «вхід через Google»; owner 2026-10-18 16:33 «давай, починай»).
 *
 * Sign-in only shows who is signed in — it locks nothing: own volumes, import and reading
 * work exactly as without it. The closed catalogue for friends is the next step.
 *
 * Nothing secret is kept: only the e-mail and name for the card. The Google ID token is not
 * stored — the closed catalogue will ask for a fresh one when it needs to prove who you are.
 */
object GoogleAccountConfig {
    /**
     * Web client ID of the owner's Google Cloud project (`GOOGLE_WEB_CLIENT_ID` in
     * build.gradle.kts). Not a secret — Google client IDs are public. Empty until the owner
     * creates the project; the card then says sign-in is not set up yet.
     */
    val webClientId: String get() = BuildConfig.GOOGLE_WEB_CLIENT_ID.trim()

    val isConfigured: Boolean get() = isValidWebClientId(webClientId)

    fun isValidWebClientId(id: String): Boolean =
        id.length > WEB_CLIENT_SUFFIX.length + 8 &&
            id.endsWith(WEB_CLIENT_SUFFIX) &&
            id.none { it.isWhitespace() }

    private const val WEB_CLIENT_SUFFIX = ".apps.googleusercontent.com"

    const val NOT_CONFIGURED_TEXT =
        "Вхід через Google ще не налаштовано: автор має один раз створити проєкт у Google Cloud. " +
            "Застосунок працює й без входу."
}

data class GoogleAccountInfo(
    val email: String,
    val name: String,
    val signedInAtMs: Long,
) {
    /** «Ім'я · пошта», або лише пошта, якщо Google не дав імені. */
    fun label(): String = if (name.isBlank() || name == email) email else "$name · $email"
}

/** The signed-in account for the card: e-mail and name only (no token). */
class GoogleAccountStore(
    context: Context,
) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun current(): GoogleAccountInfo? {
        val email = prefs.getString(KEY_EMAIL, null)?.trim().orEmpty()
        if (email.isEmpty()) return null
        return GoogleAccountInfo(
            email = email,
            name = prefs.getString(KEY_NAME, null).orEmpty(),
            signedInAtMs = prefs.getLong(KEY_SIGNED_IN_AT, 0L),
        )
    }

    fun save(info: GoogleAccountInfo): Boolean =
        prefs.edit()
            .putString(KEY_EMAIL, info.email)
            .putString(KEY_NAME, info.name)
            .putLong(KEY_SIGNED_IN_AT, info.signedInAtMs)
            .commit()

    fun clear(): Boolean = prefs.edit().clear().commit()

    companion object {
        private const val PREFS = "renodocs_google_account"
        private const val KEY_EMAIL = "email"
        private const val KEY_NAME = "name"
        private const val KEY_SIGNED_IN_AT = "signed_in_at"
    }
}

/**
 * Sign-in / sign-out through Android Credential Manager («Увійти через Google» — Google's own
 * window). The result may arrive after the screen was rotated, so it goes to whatever screen
 * listens now ([listener]); with no screen it waits in [pendingMessage].
 */
object GoogleAccountFlow {
    @Volatile
    var busy: Boolean = false
        private set

    /** The visible Settings screen: called on the main thread with a message for the status line. */
    var listener: ((String) -> Unit)? = null
        set(value) {
            field = value
            val waiting = pendingMessage
            if (value != null && waiting != null) {
                pendingMessage = null
                value(waiting)
            }
        }

    private var pendingMessage: String? = null
    private val main = Handler(Looper.getMainLooper())

    private fun report(message: String) {
        main.post {
            val l = listener
            if (l != null) l(message) else pendingMessage = message
        }
    }

    fun signIn(activity: Activity) {
        if (!GoogleAccountConfig.isConfigured) {
            report(GoogleAccountConfig.NOT_CONFIGURED_TEXT)
            return
        }
        if (busy) return
        busy = true
        val appContext = activity.applicationContext
        val request =
            GetCredentialRequest.Builder()
                .addCredentialOption(
                    GetSignInWithGoogleOption.Builder(GoogleAccountConfig.webClientId).build(),
                )
                .build()
        try {
            CredentialManager.create(activity).getCredentialAsync(
                activity,
                request,
                CancellationSignal(),
                ContextCompat.getMainExecutor(activity),
                object : CredentialManagerCallback<GetCredentialResponse, GetCredentialException> {
                    override fun onResult(result: GetCredentialResponse) {
                        busy = false
                        report(onCredential(appContext, result))
                    }

                    override fun onError(e: GetCredentialException) {
                        busy = false
                        report(explain(e))
                    }
                },
            )
        } catch (error: Throwable) {
            busy = false
            report("Не вдалося відкрити вхід Google: " + (error.message ?: error.javaClass.simpleName))
        }
    }

    private fun onCredential(
        context: Context,
        result: GetCredentialResponse,
    ): String {
        val credential = result.credential
        if (credential !is CustomCredential ||
            credential.type != GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
        ) {
            return "Google повернув незнайомий тип входу — нічого не змінено."
        }
        return try {
            val google = GoogleIdTokenCredential.createFrom(credential.data)
            val info =
                GoogleAccountInfo(
                    email = google.id,
                    name = google.displayName.orEmpty(),
                    signedInAtMs = System.currentTimeMillis(),
                )
            GoogleAccountStore(context).save(info)
            "Увійдено: " + info.label() + ". Томи працюють як і раніше."
        } catch (error: Throwable) {
            "Не вдалося прочитати відповідь Google: " + (error.message ?: error.javaClass.simpleName)
        }
    }

    private fun explain(e: GetCredentialException): String =
        when (e) {
            is GetCredentialCancellationException -> "Вхід скасовано — нічого не змінено."
            is NoCredentialException ->
                "Не знайдено акаунта Google для входу. Додай акаунт у налаштуваннях телефона й спробуй ще раз."
            is GetCredentialProviderConfigurationException ->
                "На телефоні нема служб Google, потрібних для входу."
            else -> "Не вдалося увійти: " + (e.errorMessage?.toString()?.takeIf { it.isNotBlank() } ?: e.type)
        }

    /** «Вийти»: the card forgets the account at once; Google's saved choice is cleared too. */
    fun signOut(activity: Activity) {
        GoogleAccountStore(activity).clear()
        val done = "Вийшов з акаунта Google. Томи й проєкти лишились, як були."
        try {
            CredentialManager.create(activity).clearCredentialStateAsync(
                ClearCredentialStateRequest(),
                CancellationSignal(),
                ContextCompat.getMainExecutor(activity),
                object : CredentialManagerCallback<Void?, ClearCredentialException> {
                    override fun onResult(result: Void?) = report(done)

                    override fun onError(e: ClearCredentialException) = report(done)
                },
            )
        } catch (error: Throwable) {
            report(done)
        }
    }
}
