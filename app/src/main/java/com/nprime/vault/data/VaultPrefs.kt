package com.nprime.vault.data

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import org.json.JSONArray
import java.security.MessageDigest
import java.security.SecureRandom

object VaultPrefs {

    private const val FILE_NAME = "vault_secure_prefs"
    private const val KEY_SETUP_COMPLETE = "setup_complete"
    private const val KEY_REAL_PIN_HASH = "real_pin_hash"
    private const val KEY_REAL_PIN_SALT = "real_pin_salt"
    private const val KEY_DURESS_PIN_HASH = "duress_pin_hash"
    private const val KEY_DURESS_PIN_SALT = "duress_pin_salt"
    private const val KEY_SELECTED_APPS = "selected_apps"
    private const val KEY_SELECTED_FILES = "selected_files"
    private const val KEY_LOCK_ENABLED = "lock_enabled"
    private const val KEY_WIPE_SELF = "wipe_self"
    private const val KEY_ONBOARDING_DONE = "onboarding_done"

    @Volatile private var prefs: SharedPreferences? = null

    private fun get(context: Context): SharedPreferences {
        return prefs ?: synchronized(this) {
            prefs ?: build(context).also { prefs = it }
        }
    }

    private fun build(context: Context): SharedPreferences {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        return EncryptedSharedPreferences.create(
            context,
            FILE_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    // ── Setup state ──────────────────────────────────────────────────────────

    fun isSetupComplete(ctx: Context) = get(ctx).getBoolean(KEY_SETUP_COMPLETE, false)
    fun markSetupComplete(ctx: Context) = get(ctx).edit().putBoolean(KEY_SETUP_COMPLETE, true).apply()

    fun isOnboardingDone(ctx: Context) = get(ctx).getBoolean(KEY_ONBOARDING_DONE, false)
    fun markOnboardingDone(ctx: Context) = get(ctx).edit().putBoolean(KEY_ONBOARDING_DONE, true).apply()

    // ── Lock enabled ─────────────────────────────────────────────────────────

    fun isLockEnabled(ctx: Context) = get(ctx).getBoolean(KEY_LOCK_ENABLED, false)
    fun setLockEnabled(ctx: Context, enabled: Boolean) =
        get(ctx).edit().putBoolean(KEY_LOCK_ENABLED, enabled).apply()

    fun shouldWipeSelf(ctx: Context) = get(ctx).getBoolean(KEY_WIPE_SELF, false)
    fun setWipeSelf(ctx: Context, v: Boolean) = get(ctx).edit().putBoolean(KEY_WIPE_SELF, v).apply()

    // ── PIN management ───────────────────────────────────────────────────────

    private fun randomSalt(): String {
        val bytes = ByteArray(16)
        SecureRandom().nextBytes(bytes)
        return bytes.joinToString("") { "%02x".format(it) }
    }

    private fun hash(pin: String, salt: String): String {
        val input = "$salt:$pin".toByteArray(Charsets.UTF_8)
        val digest = MessageDigest.getInstance("SHA-256").digest(input)
        return digest.joinToString("") { "%02x".format(it) }
    }

    fun saveRealPin(ctx: Context, pin: String) {
        val salt = randomSalt()
        get(ctx).edit()
            .putString(KEY_REAL_PIN_SALT, salt)
            .putString(KEY_REAL_PIN_HASH, hash(pin, salt))
            .apply()
    }

    fun saveDuressPin(ctx: Context, pin: String) {
        val salt = randomSalt()
        get(ctx).edit()
            .putString(KEY_DURESS_PIN_SALT, salt)
            .putString(KEY_DURESS_PIN_HASH, hash(pin, salt))
            .apply()
    }

    fun checkRealPin(ctx: Context, pin: String): Boolean {
        val salt = get(ctx).getString(KEY_REAL_PIN_SALT, null) ?: return false
        val stored = get(ctx).getString(KEY_REAL_PIN_HASH, null) ?: return false
        return hash(pin, salt) == stored
    }

    fun checkDuressPin(ctx: Context, pin: String): Boolean {
        val salt = get(ctx).getString(KEY_DURESS_PIN_SALT, null) ?: return false
        val stored = get(ctx).getString(KEY_DURESS_PIN_HASH, null) ?: return false
        return hash(pin, salt) == stored
    }

    fun hasPinsConfigured(ctx: Context): Boolean {
        val p = get(ctx)
        return p.getString(KEY_REAL_PIN_HASH, null) != null &&
                p.getString(KEY_DURESS_PIN_HASH, null) != null
    }

    // ── Target lists ─────────────────────────────────────────────────────────

    fun getSelectedApps(ctx: Context): List<String> {
        val raw = get(ctx).getString(KEY_SELECTED_APPS, null) ?: return emptyList()
        return try {
            val arr = JSONArray(raw)
            List(arr.length()) { arr.getString(it) }
        } catch (_: Exception) { emptyList() }
    }

    fun saveSelectedApps(ctx: Context, packages: List<String>) {
        val arr = JSONArray(packages)
        get(ctx).edit().putString(KEY_SELECTED_APPS, arr.toString()).apply()
    }

    fun getSelectedFiles(ctx: Context): List<String> {
        val raw = get(ctx).getString(KEY_SELECTED_FILES, null) ?: return emptyList()
        return try {
            val arr = JSONArray(raw)
            List(arr.length()) { arr.getString(it) }
        } catch (_: Exception) { emptyList() }
    }

    fun saveSelectedFiles(ctx: Context, paths: List<String>) {
        val arr = JSONArray(paths)
        get(ctx).edit().putString(KEY_SELECTED_FILES, arr.toString()).apply()
    }

    fun clearAll(ctx: Context) {
        get(ctx).edit().clear().apply()
    }
}
