package com.saney.renaultdocs

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.AuthorizationResult
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.common.api.Scope
import com.google.android.gms.tasks.Tasks
import java.util.concurrent.TimeUnit

/**
 * 0.6.6 (#133, step «закритий каталог для друзів», option (а) — owner 2026-10-18 18:31
 * «давай А попробуємо»).
 *
 * A friend = a Google account the owner gave access to the catalogue folder on his Drive.
 * Renodocs reads the catalogue and the volumes through the Drive API with the signed-in
 * account's permission «read files on Google Drive» (`drive.readonly`). Nothing is built
 * into the APK: no list of friends, no keys.
 *
 * While the folder is still open for «anyone with the link», the old public way keeps
 * working as a fallback, so nothing breaks before the owner closes the folder.
 *
 * The access token lives only in memory (about an hour; Google refreshes it silently).
 * It is never written to disk.
 */
object DriveAccess {
    const val SCOPE = "https://www.googleapis.com/auth/drive.readonly"

    /** Google access tokens live ~60 min; take a fresh one well before that. */
    private const val TOKEN_FRESH_MS = 45L * 60L * 1000L
    private const val SILENT_TIMEOUT_S = 30L

    @Volatile
    private var token: String? = null

    @Volatile
    private var tokenAtMs: Long = 0L

    private fun request(): AuthorizationRequest =
        AuthorizationRequest.builder()
            .setRequestedScopes(listOf(Scope(SCOPE)))
            .build()

    fun isSignedIn(context: Context): Boolean = GoogleAccountStore(context).current() != null

    /** The token in memory, if it is still fresh. */
    fun cachedToken(): String? {
        val t = token ?: return null
        return if (System.currentTimeMillis() - tokenAtMs < TOKEN_FRESH_MS) t else null
    }

    fun remember(accessToken: String?) {
        if (accessToken.isNullOrBlank()) return
        token = accessToken
        tokenAtMs = System.currentTimeMillis()
    }

    /** Called on «Вийти» and when Google says the token is no longer valid. */
    fun forget() {
        token = null
        tokenAtMs = 0L
    }

    /**
     * Background thread only. A token without showing anything: from memory, or from Google
     * when the permission was already given. Null when nobody is signed in or Google needs
     * to ask the person first (the catalogue screen asks; the import service cannot).
     */
    fun tokenSilently(context: Context): String? {
        cachedToken()?.let { return it }
        if (!isSignedIn(context)) return null
        return try {
            val result =
                Tasks.await(
                    Identity.getAuthorizationClient(context.applicationContext).authorize(request()),
                    SILENT_TIMEOUT_S,
                    TimeUnit.SECONDS,
                )
            if (result.hasResolution()) null else result.accessToken.also { remember(it) }
        } catch (error: Throwable) {
            null
        }
    }

    /**
     * Main thread. Asks Google for the permission; if Google must show its window,
     * [onNeedsWindow] gets the sender (the screen starts it and passes the result to
     * [tokenFromResult]). [onDone] gets the token, or null with a reason.
     */
    fun ask(
        activity: Activity,
        onNeedsWindow: (IntentSender) -> Unit,
        onDone: (String?, String?) -> Unit,
    ) {
        try {
            Identity.getAuthorizationClient(activity)
                .authorize(request())
                .addOnSuccessListener { result: AuthorizationResult ->
                    val sender = result.pendingIntent?.intentSender
                    if (result.hasResolution() && sender != null) {
                        onNeedsWindow(sender)
                    } else {
                        remember(result.accessToken)
                        onDone(result.accessToken, null)
                    }
                }
                .addOnFailureListener { error ->
                    onDone(null, error.message ?: error.javaClass.simpleName)
                }
        } catch (error: Throwable) {
            onDone(null, error.message ?: error.javaClass.simpleName)
        }
    }

    /** The answer from Google's window ([ask] → [onNeedsWindow]). */
    fun tokenFromResult(
        activity: Activity,
        data: Intent?,
    ): String? {
        val answer = data ?: return null
        return try {
            Identity.getAuthorizationClient(activity)
                .getAuthorizationResultFromIntent(answer)
                .accessToken
                .also { remember(it) }
        } catch (error: Throwable) {
            null
        }
    }
}

/** How the catalogue was reached — shown next to «Каталог vN». */
enum class DriveVia {
    ACCOUNT,
    PUBLIC,
}

/** The catalogue or a volume is not reachable: a message for the person, in Ukrainian. */
class DriveAccessException(
    message: String,
) : IllegalStateException(message)

object DriveAccessTexts {
    const val CLOSED_SIGN_IN =
        "Каталог закритий. Увійди через Google (Налаштування → «Акаунт Google») — " +
            "і попроси автора відкрити каталог для твоєї пошти."

    fun noAccess(email: String?): String =
        "Акаунт " + (email?.takeIf { it.isNotBlank() } ?: "Google") +
            " не має доступу до каталогу. Попроси автора відкрити каталог для цієї пошти."

    const val SESSION_ENDED =
        "Дозвіл Google закінчився. Відкрий «Готові проєкти» ще раз — Google спитає знову."

    const val API_REFUSED =
        "Google не дав прочитати каталог через акаунт (немає дозволу або в проєкті Google Cloud " +
            "не ввімкнено Google Drive API)."
}
