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

    private const val KEY_REAL_PIN_HASH  = "real_pin_hash"
    private const val KEY_REAL_PIN_SALT  = "real_pin_salt"
    private const val KEY_DURESS_PIN_HASH = "duress_pin_hash"
    private const val KEY_DURESS_PIN_SALT = "duress_pin_salt"
    private const val KEY_SELECTED_APPS  = "selected_apps"
    private const val KEY_SELECTED_FILES = "selected_files"
    private const val KEY_LOCK_ENABLED   = "lock_enabled"
    private const val KEY_SETUP_COMPLETE = "setup_complete"
    private const val KEY_FAILED_ATTEMPTS = "failed_attempts"
    private const val KEY_LOCKOUT_UNTIL  = "lockout_until"

    private fun prefs(context: Context): SharedPreferences {
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

    // ── PIN hashing ───────────────────────────────────────────────────────────

    private fun generateSalt(): String {
        val bytes = ByteArray(32)
        SecureRandom().nextBytes(bytes)
        return bytes.joinToString("") { "%02x".format(it) }
    }

    private fun hashPin(pin: String, salt: String): String {
        val input = (salt + pin).toByteArray(Charsets.UTF_8)
        val digest = MessageDigest.getInstance("SHA-256").digest(input)
        return digest.joinToString("") { "%02x".format(it) }
    }

    // ── PIN storage ───────────────────────────────────────────────────────────

    fun saveRealPin(context: Context, pin: String) {
        val salt = generateSalt()
        val hash = hashPin(pin, salt)
        prefs(context).edit()
            .putString(KEY_REAL_PIN_HASH, hash)
            .putString(KEY_REAL_PIN_SALT, salt)
            .apply()
    }

    fun saveDuressPin(context: Context, pin: String) {
        val salt = generateSalt()
        val hash = hashPin(pin, salt)
        prefs(context).edit()
            .putString(KEY_DURESS_PIN_HASH, hash)
            .putString(KEY_DURESS_PIN_SALT, salt)
            .apply()
    }

    fun checkRealPin(context: Context, pin: String): Boolean {
        val p = prefs(context)
        val salt = p.getString(KEY_REAL_PIN_SALT, null) ?: return false
        val hash = p.getString(KEY_REAL_PIN_HASH, null) ?: return false
        return hashPin(pin, salt) == hash
    }

    fun checkDuressPin(context: Context, pin: String): Boolean {
        val p = prefs(context)
        val salt = p.getString(KEY_DURESS_PIN_SALT, null) ?: return false
        val hash = p.getString(KEY_DURESS_PIN_HASH, null) ?: return false
        return hashPin(pin, salt) == hash
    }

    fun hasPinsSet(context: Context): Boolean {
        val p = prefs(context)
        return p.contains(KEY_REAL_PIN_HASH) && p.contains(KEY_DURESS_PIN_HASH)
    }

    // ── Lockout ───────────────────────────────────────────────────────────────

    fun recordFailedAttempt(context: Context): Int {
        val p = prefs(context)
        val attempts = p.getInt(KEY_FAILED_ATTEMPTS, 0) + 1
        val lockoutUntil = when {
            attempts >= 10 -> System.currentTimeMillis() + 5 * 60_000L  // 5 min
            attempts >= 7  -> System.currentTimeMillis() + 60_000L       // 1 min
            attempts >= 5  -> System.currentTimeMillis() + 30_000L       // 30 sec
            else           -> 0L
        }
        p.edit()
            .putInt(KEY_FAILED_ATTEMPTS, attempts)
            .putLong(KEY_LOCKOUT_UNTIL, lockoutUntil)
            .apply()
        return attempts
    }

    fun clearFailedAttempts(context: Context) {
        prefs(context).edit()
            .putInt(KEY_FAILED_ATTEMPTS, 0)
            .putLong(KEY_LOCKOUT_UNTIL, 0L)
            .apply()
    }

    fun getLockoutUntil(context: Context): Long =
        prefs(context).getLong(KEY_LOCKOUT_UNTIL, 0L)

    fun getFailedAttempts(context: Context): Int =
        prefs(context).getInt(KEY_FAILED_ATTEMPTS, 0)

    // ── Targets ───────────────────────────────────────────────────────────────

    fun saveSelectedApps(context: Context, packageNames: Set<String>) {
        val json = JSONArray(packageNames.toList()).toString()
        prefs(context).edit().putString(KEY_SELECTED_APPS, json).apply()
    }

    fun getSelectedApps(context: Context): Set<String> {
        val json = prefs(context).getString(KEY_SELECTED_APPS, null) ?: return emptySet()
        val arr = JSONArray(json)
        return (0 until arr.length()).map { arr.getString(it) }.toSet()
    }

    fun saveSelectedFiles(context: Context, paths: Set<String>) {
        val json = JSONArray(paths.toList()).toString()
        prefs(context).edit().putString(KEY_SELECTED_FILES, json).apply()
    }

    fun getSelectedFiles(context: Context): Set<String> {
        val json = prefs(context).getString(KEY_SELECTED_FILES, null) ?: return emptySet()
        val arr = JSONArray(json)
        return (0 until arr.length()).map { arr.getString(it) }.toSet()
    }

    // ── State flags ───────────────────────────────────────────────────────────

    fun setLockEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_LOCK_ENABLED, enabled).apply()
    }

    fun isLockEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_LOCK_ENABLED, false)

    fun markSetupComplete(context: Context) {
        prefs(context).edit().putBoolean(KEY_SETUP_COMPLETE, true).apply()
    }

    fun isSetupComplete(context: Context): Boolean =
        prefs(context).getBoolean(KEY_SETUP_COMPLETE, false)
}
