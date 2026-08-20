# Vault

A security app disguised as **System Settings** that replaces the Android lock screen entirely using Device Owner (DO) APIs.

- Real password → unlocks the device normally
- Duress password → silently wipes selected apps and files, then unlocks
- Configurable failed-attempt limit → factory reset on threshold
- Looks identical to stock Pixel Settings

---

## Device Owner Setup

Device Owner must be provisioned **before** any Google account is added to the device, and cannot be set from within the app itself.

### Option 1 — USB (ADB)

```bash
# 1. Remove all Google accounts: Settings → Accounts → remove each one
# 2. Connect USB cable
adb shell dpm set-device-owner com.nprime.vault/.admin.DeviceOwnerReceiver
```

### Option 2 — ADB over Wi-Fi (no USB cable)

```bash
# On phone: Settings → Developer Options → Wireless Debugging → enable
# Tap "Pair device with pairing code" — note the IP:port and 6-digit code

adb pair <ip>:<port> <pairing-code>
adb connect <ip>:<port>
adb shell dpm set-device-owner com.nprime.vault/.admin.DeviceOwnerReceiver
```

> **Note:** Developer Options must already be enabled (tap Build Number 7 times in About Phone).

### Verify

```bash
adb shell dpm list-owners
# Should show: com.nprime.vault/.admin.DeviceOwnerReceiver
```

### Remove Device Owner (development only)

```bash
adb shell dpm remove-active-admin com.nprime.vault/.admin.DeviceOwnerReceiver
```

---

## Setup Flow (in-app)

After installing and provisioning DO, open Vault and complete the four setup steps:

1. **Provision Device Owner** — verified via ADB (above)
2. **Remove System Lock Screen** — Settings → Security → Screen Lock → None
3. **Allow Display Over Other Apps** — grant overlay permission
4. **Set Unlock Passwords** — real password + duress password

Vault's lock screen activates automatically once all steps are complete.

---

## Build & Install

```bash
./gradlew installDebug
```

Requires Android SDK 35, Gradle 8+, and a connected device/emulator.

---

## Architecture

| Component | Purpose |
|---|---|
| `DeviceOwnerReceiver` | Device Policy Controller (DPC) entry point |
| `DeviceOwnerManager` | Wraps all `DevicePolicyManager` calls |
| `LockOverlayService` | Foreground service that draws the lock screen overlay |
| `SilentWipeService` | Orchestrates duress wipe: suspend apps → uninstall → delete files → unsuspend |
| `VaultPrefs` | Encrypted shared preferences (AES256-GCM) for password hashes and config |
| `BootReceiver` | Restarts `LockOverlayService` after device reboot |
