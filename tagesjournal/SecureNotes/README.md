# Secure Notes 🔒

A personal notes app for Android 16+ that stores all data in an **AES-256 encrypted SQLite database** (SQLCipher), unlocked exclusively via **fingerprint authentication** backed by the Android Keystore.

---

## Security Architecture

```
Fingerprint sensor
      │
      ▼
Android Keystore (AES-256, hardware-backed)
      │  auth-gated — key unusable without biometric
      ▼
Decrypt 32-byte random DB passphrase
(stored encrypted in EncryptedSharedPreferences)
      │
      ▼
SQLCipher opens secure_notes.db with passphrase
      │
      ▼
Room DAO  ←→  ViewModel  ←→  UI Fragments
```

**Key properties:**
- The raw DB passphrase is **never written to disk** in plaintext.
- The Keystore key is **invalidated** if a new fingerprint is enrolled.
- Cloud backup and device-transfer are **disabled** for all sensitive files.
- Passphrase bytes are **zeroed in memory** immediately after DB init.

---

## Project Structure

```
app/src/main/java/com/securenotes/app/
├── SecureNotesApp.kt          — Hilt application class
├── data/
│   ├── Note.kt                — Room entity + DAO
│   ├── NotesDatabase.kt       — SQLCipher-encrypted Room DB
│   └── NoteRepository.kt      — data access layer
├── di/
│   ├── AppModule.kt           — Hilt DI bindings
│   └── DatabaseHolder.kt      — lazy DB init after auth
├── security/
│   ├── CryptoManager.kt       — Keystore key + cipher operations
│   └── PassphraseManager.kt   — encrypted passphrase storage
├── ui/
│   ├── MainActivity.kt        — single-activity host
│   ├── LockFragment.kt        — biometric prompt + unlock flow
│   ├── NotesFragment.kt       — notes list + search + swipe-delete
│   ├── NoteEditorFragment.kt  — create / edit a note
│   └── NoteAdapter.kt         — RecyclerView adapter
└── viewmodel/
    ├── LockViewModel.kt        — auth state machine
    ├── NotesViewModel.kt       — notes list + search
    └── NoteEditorViewModel.kt  — note CRUD
```

---

## Setup

### Requirements
- **Android Studio Hedgehog** (2023.1.1) or newer
- **Android SDK 36** (Android 16) installed
- A physical Android 16 device **or** an AVD with API 36 + fingerprint support

### Steps

1. **Unzip** the project and open the root folder in Android Studio.
2. Open `local.properties` and set your SDK path:
   ```
   sdk.dir=/Users/YourName/Library/Android/sdk
   ```
3. Let Gradle sync finish (it will download all dependencies automatically).
4. **Run** on a physical device or emulator with a fingerprint enrolled.

> **Emulator tip:** In the AVD settings enable *Advanced → Fingerprint*, then enroll via  
> *Settings → Security → Fingerprint* on the emulator.

---

## Dependencies

| Library | Purpose |
|---|---|
| SQLCipher 4.5 | AES-256 database encryption |
| Room 2.6 | SQLite ORM |
| AndroidX Biometric | Fingerprint / strong-biometric prompt |
| Android Keystore | Hardware-backed crypto key storage |
| EncryptedSharedPreferences | Secure passphrase persistence |
| Hilt 2.51 | Dependency injection |
| Navigation Component | Fragment navigation + SafeArgs |
| Material 3 | UI components |

---

## Notes & Limitations

- **Minimum SDK: 36** (Android 16). The `AUTH_BIOMETRIC_STRONG` + per-use auth requires API 30+, but targeting 36 aligns with your requirement.
- If the user adds a new fingerprint, the Keystore key is **invalidated** — they will be prompted to re-authenticate and a new key + passphrase will be generated (existing notes will be inaccessible). Consider adding a migration/export flow for production use.
- This project does not include a fallback PIN/password; for production, consider adding `DEVICE_CREDENTIAL` as a fallback authenticator.
