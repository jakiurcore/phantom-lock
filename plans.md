# Vault — Fresh Build Plan

Implementation Plan · Kotlin + Jetpack Compose · Device Owner

Clean project, built from scratch with Kotlin and Jetpack Compose. Device Owner is the foundation — not an add-on.

## Goals

| # | Goal |
|---|------|
| 01 | User selects apps + files; all removed silently on duress password |
| 02 | Device Owner replaces system keyguard — bypassing requires factory reset |
| 03 | Silent wipe during unlock animation — nothing suspicious visible |
| 04 | App is the primary lock — active from first boot, no manual trigger |

---

## Project Setup

### PH 00 — Build System: Kotlin + Compose ✅

Wire up Gradle for Compose before writing a single line of app code.

- [x] 0-1 Add Kotlin + Compose Gradle plugins to `libs.versions.toml`
  - Versions: `kotlin = "2.1.21"`, `composeBom = "2025.06.01"`. Plugins: `org.jetbrains.kotlin.android` and `org.jetbrains.kotlin.plugin.compose`.
- [x] 0-2 Update root `build.gradle.kts` — declare Kotlin plugins `apply false`
- [x] 0-3 Update `app/build.gradle.kts` — enable `compose = true`, add Compose BOM dependencies
  - Note: AGP 9.x bundles Kotlin internally — do NOT apply `kotlin.android` separately, only apply `android.application` + `kotlin.compose`.
- [x] 0-4 Verify project syncs and compiles with an empty `MainActivity`

---

### PH 00b — Device Owner Provisioning ✅

Provision the device as Device Owner early so all subsequent dev/testing runs with DO privileges active.

- [x] 00b-1 Write minimal `DeviceOwnerReceiver.kt`
  - Extends `DeviceAdminReceiver`. `onEnabled()` calls `applyPolicies()`. Lives at `admin/DeviceOwnerReceiver`.
- [x] 00b-2 Write `res/xml/device_admin_policies.xml`
  - Declares: `limit-password`, `force-lock`, `wipe-data`, `disable-keyguard-features`.
- [x] 00b-3 Declare `DeviceOwnerReceiver` in `AndroidManifest.xml`
  - With `BIND_DEVICE_ADMIN` permission and meta-data pointing to `device_admin_policies.xml`.
- [x] 00b-4 Provision via ADB on real device
  - Tip: remove all Google accounts first. Hidden service accounts (e.g. `com.google.android.apps.tachyon`) must be cleared with `adb shell pm clear <package>`.
  - Run: `adb shell dpm set-device-owner com.nprime.vault/.admin.DeviceOwnerReceiver`
- [x] 00b-5 Verify DO is active
  - `adb shell dpm list-owners` shows `com.nprime.vault/.admin.DeviceOwnerReceiver`.

> **Warning:** Once set as Device Owner, the app cannot be uninstalled without calling `clearDeviceOwner()` or factory resetting the device.

---

## Foundation

### PH 01 — Package Structure & Data Layer ✅

- [x] 1-1 Lock in the package structure
  ```
  admin/          DeviceOwnerManager, DeviceOwnerReceiver
  data/           VaultPrefs
  receiver/       BootReceiver
  service/        LockOverlayService, SilentWipeService
  ui/theme/       Color, Theme, Type
  ui/components/  PasswordField
  ui/navigation/  AppNavigation
  ui/lock/        LockScreen
  ui/setup/       SetupScreen, SetupViewModel
  ui/home/        HomeScreen, HomeViewModel
  ui/pinsetup/    PasswordSetupScreen, PasswordSetupViewModel
  ui/targets/     TargetsScreen, TargetsViewModel
  ```
- [x] 1-2 Write `VaultPrefs.kt` — encrypted SharedPreferences wrapper
  - AES256-GCM encrypted prefs, SHA-256 + random salt password hashing, exponential lockout (5 wrong = 30s, 7 = 1min, 10 = 5min), selected apps/files as JSON sets.
- [x] 1-3 Write `DeviceOwnerManager.kt`
  - `isDeviceOwner()`, `applyPolicies()` (keyguard disabled + user restrictions), `setStatusBarLocked()`, `clearDeviceOwner()`.
- [x] 1-4 Write `DeviceOwnerReceiver.kt` + `res/xml/device_admin_policies.xml`
  - `onEnabled()` calls `applyPolicies()`.

---

## Design System

### PH 02 — Theme & Design System ✅

- [x] 2-1 Write `Color.kt` — stock Pixel lock screen palette
  - True black background, white text, dark gray surfaces, soft red for errors. No "security app" colors.
- [x] 2-2 Write `Type.kt` and `Theme.kt` — `VaultTheme` wrapping Material3 `darkColorScheme`
- [x] 2-3 Write `PasswordField.kt` as shared Composable
  - Secure text input, show/hide toggle, shake + haptic on wrong password, auto-focuses keyboard. Min 6 chars.

---

## Core Services

### PH 03 — Lock Overlay Service

The heart of the app — a foreground Service that draws a Compose UI directly onto the WindowManager.

- [ ] 3-1 Implement `LockOverlayService` with `LifecycleOwner`, `ViewModelStoreOwner`, `SavedStateRegistryOwner`
  - ComposeView inside a Service requires these three owners implemented on the Service itself.
- [ ] 3-2 Register `SCREEN_ON` / `SCREEN_OFF` broadcast receivers inside the service
  - `SCREEN_OFF` → set `shouldLock = true`. `SCREEN_ON` → if `shouldLock`, call `showOverlay()`. Must be dynamic, not manifest.
- [ ] 3-3 Build the `ComposeView` with correct `WindowManager.LayoutParams`
  - Type: `TYPE_APPLICATION_OVERLAY`. Flags: `FLAG_LAYOUT_IN_SCREEN | FLAG_LAYOUT_NO_LIMITS | FLAG_SECURE`. Covers status bar + nav bar.
- [ ] 3-4 Intercept hardware keys in the overlay view's `setOnKeyListener`
  - Consume: Back, Volume Up/Down, Menu, Camera, Search, Power. Return `true` to swallow all.
- [ ] 3-5 Implement password evaluation — real password → dismiss, duress password → start wipe
  - State as `MutableStateFlow<LockUiState>` on companion object. Wrong password: increment lockout counter, shake field.
- [ ] 3-6 Call `DeviceOwnerManager.setStatusBarLocked(true/false)` on show/dismiss

---

### PH 04 — Silent Wipe Service

PackageInstaller-based silent uninstall. No dialogs, no accessibility service.

- [ ] 4-1 Write `SilentWipeService` as a foreground `Service` with a coroutine scope
- [ ] 4-2 Implement silent uninstall via `PackageInstaller.uninstall(VersionedPackage, IntentSender)`
  - `CompletableDeferred` + `withTimeoutOrNull(12_000)` per app. Device Owner = no confirmation dialog.
- [ ] 4-3 Implement file deletion — `File.deleteRecursively()` for directories
- [ ] 4-4 Broadcast `ACTION_WIPE_COMPLETE` when done — overlay receives it and dismisses

---

### PH 05 — Boot Receiver

Lock must be visible on first screen-on after every reboot, before any user interaction.

- [ ] 5-1 Write `BootReceiver` for `BOOT_COMPLETED`
  - Guard: only act if `VaultPrefs.isSetupComplete()` and `VaultPrefs.isLockEnabled()`. Re-apply DO policies, then start service.
- [ ] 5-2 Add `ACTION_SHOW_NOW` intent action to `LockOverlayService`
  - Triggers `showOverlay()` immediately on `onStartCommand` for boot scenario.

---

## UI Screens

### PH 06 — Lock Screen

Full-screen Compose UI shown inside the overlay. Looks like a system lock screen.

- [ ] 6-1 Implement `LockScreen` composable — clock, password field
  - Large light-weight clock + date at top, password field center. True black background. Keyboard auto-shows.
- [ ] 6-2 Implement wiping state — spinner + neutral message ("Decrypting device…")
  - While `isWiping == true`, show spinner instead of password field. Looks like a slow unlock.
- [ ] 6-3 Wire state via `StateFlow` from `LockOverlayService` companion

---

### PH 07 — Setup Screen

First-run flow: confirm Device Owner → set real password → set duress password → done.

- [ ] 7-1 Show Device Owner status card with ADB command + copy button + "Check status" button
  - "Continue" button only enabled when DO is confirmed active.
- [ ] 7-2 Navigate to password setup (real) → password setup (duress) → mark setup complete → home

---

### PH 08 — Password Setup Screen

Two-step entry + confirmation. Reused for both real and duress password.

- [ ] 8-1 Write `PasswordSetupViewModel` — two-step enter + confirm flow
  - Step 0: enter password → store as `firstEntry`. Step 1: enter again → mismatch resets with error, match saves.
- [ ] 8-2 Write `PasswordSetupScreen` composable using the shared `PasswordField`

---

### PH 09 — Home Screen

Main control panel after setup. Enable/disable lock, manage targets, deactivate.

- [ ] 9-1 Status row — Device Owner active/inactive, lock enabled/disabled
- [ ] 9-2 Enable/Disable lock toggle — starts/stops `LockOverlayService`
- [ ] 9-3 Targets summary card — "N apps, M files selected" → taps navigate to TargetsScreen
- [ ] 9-4 "Change password" and "Change Duress password" buttons → navigate to PasswordSetupScreen
- [ ] 9-5 "Deactivate Vault" — confirmation dialog → `DeviceOwnerManager.clearDeviceOwner()`

---

### PH 10 — Targets Screen

App selection (checkbox list) and file selection (path-based picker).

- [ ] 10-1 Tab layout — Apps | Files
- [ ] 10-2 Apps tab — load installed user apps via `PackageManager`, show with icon + name + checkbox
  - Coroutine on IO dispatcher. Filter system apps. Sort alphabetically. Persist on each toggle.
- [ ] 10-3 Files tab — `OpenDocumentTree` picker + list of selected paths with remove button

---

### PH 11 — Navigation & Entry Point

Single-activity Compose navigation. Start destination determined by setup state.

- [ ] 11-1 Write `AppNavigation.kt` — `NavHost` with routes: setup, passwordSetup/{mode}, home, targets
  - Start destination: `setup` if `!VaultPrefs.isSetupComplete()`, otherwise `home`.
- [ ] 11-2 Write `MainActivity` — single `ComponentActivity` hosting `VaultTheme { AppNavigation() }`

---

## Manifest & Hardening

### PH 12 — AndroidManifest & Permissions

- [ ] 12-1 Permissions: `SYSTEM_ALERT_WINDOW`, `RECEIVE_BOOT_COMPLETED`, `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_SPECIAL_USE`, `MANAGE_EXTERNAL_STORAGE`, `QUERY_ALL_PACKAGES`, `VIBRATE`
- [ ] 12-2 Declare `LockOverlayService` with `foregroundServiceType="specialUse"` and `directBootAware="true"`
- [ ] 12-3 Declare `SilentWipeService` with `foregroundServiceType="dataSync"`
- [ ] 12-4 Declare `BootReceiver` with `BOOT_COMPLETED` intent filter
- [ ] 12-5 Declare `DeviceOwnerReceiver` with `BIND_DEVICE_ADMIN` permission + meta-data ✅ (done in PH 00b)

---

### PH 13 — Security Hardening

User restrictions applied via Device Owner in `applyPolicies()`.

- [ ] 13-1 `DISALLOW_SAFE_BOOT` — prevent bypass via safe mode ✅ (in applyPolicies)
- [ ] 13-2 `DISALLOW_ADD_USER` — block guest/secondary profiles ✅ (in applyPolicies)
- [ ] 13-3 `DISALLOW_MOUNT_PHYSICAL_MEDIA` — block USB OTG exfiltration ✅ (in applyPolicies)
- [ ] 13-4 `DISALLOW_DEBUGGING_FEATURES` — toggleable in HomeScreen, off by default during dev
  - **Warning:** Blocks ADB. Only enable on deployment device.
- [ ] 13-5 Verify app cannot be uninstalled while Device Owner is active

---

## Verification

### TEST — End-to-End Testing Checklist

- [ ] T-1  Project builds and installs without errors
- [ ] T-2  DO provisioning via ADB succeeds — `onEnabled()` fires and policies apply
- [ ] T-3  System keyguard is absent after provisioning
- [ ] T-4  Screen off → on: overlay appears immediately, no launcher flash
- [ ] T-5  Notification shade is non-interactive while overlay is showing
- [ ] T-6  Volume, Back, Power keys are swallowed by the overlay
- [ ] T-7  Real password dismisses overlay correctly
- [ ] T-8  Duress password shows "Decrypting…" — no uninstall dialogs at any point
- [ ] T-9  Selected apps are gone after duress unlock
- [ ] T-10 Selected files are deleted after duress unlock
- [ ] T-11 After duress unlock device looks normal — no spinners, no error dialogs
- [ ] T-12 Reboot → overlay is first thing visible on screen-on
- [ ] T-13 App cannot be uninstalled while DO is active
- [ ] T-14 "Deactivate Vault" restores normal device behavior and allows uninstall
