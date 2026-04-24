# GueteTag – Android App

**GueteTag** ist eine persönliche Wohlbefindens-Tracking-App für Android.
Namespace: `ch.widmedia.guetetag` | minSdk: 36 | targetSdk: 36

---

## Funktionen

- 🔒 **Biometrische Authentifizierung** (Fingerabdruck) beim App-Start  
- 🗄️ **Verschlüsselte SQLite-Datenbank** via SQLCipher  
- 📅 **14-Tage-Kalender** mit farblicher Hervorhebung von Tagen mit Einträgen  
- ✍️ **Einträge erstellen/bearbeiten/löschen** mit Datum, Bewertung (1–10, Slider) und Notizen  
- 📤 **Export** der Datenbank als verschlüsselte `.gtb`-Datei (Passwort in Secure Storage)  
- 📥 **Import** einer solchen Datei mit Passwort  
- 🎨 Schriftarten: **Raleway** (Titel), **Nunito** (Fließtext)

---

## Setup & Kompilierung

### 1. Android Studio

Benötigt: **Android Studio Meerkat (2025.3 / Panda 4)** oder neuer.

### 2. Schriftarten herunterladen

Die Schriftart-Platzhalter in `app/src/main/res/font/` müssen durch echte TTF-Dateien ersetzt werden.  
Siehe: [`app/src/main/res/font/SCHRIFTARTEN_README.md`](app/src/main/res/font/SCHRIFTARTEN_README.md)

**Kurzanleitung:**
1. Öffne https://fonts.google.com/specimen/Raleway → „Download family"  
2. Öffne https://fonts.google.com/specimen/Nunito → „Download family"  
3. Entpacke die ZIPs und kopiere:  
   - `Raleway-Regular.ttf` → `raleway_regular.ttf`  
   - `Raleway-SemiBold.ttf` → `raleway_semibold.ttf`  
   - `Raleway-Bold.ttf` → `raleway_bold.ttf`  
   - `Nunito-Regular.ttf` → `nunito_regular.ttf`  
   - `Nunito-SemiBold.ttf` → `nunito_semibold.ttf`  
   - `Nunito-Bold.ttf` → `nunito_bold.ttf`

### 3. Projekt öffnen

```
File → Open → [dieses Verzeichnis]
```

Gradle-Sync abwarten, dann auf dem Gerät/Emulator ausführen.

### 4. Voraussetzungen Gerät/Emulator

- Android API 36+  
- Eingerichteter Fingerabdruck (Einstellungen → Sicherheit → Fingerabdruck)

---

## Architektur

```
MainActivity
├── SperrScreen          ← Biometric unlock
└── GueteTagNavigation
    ├── HauptScreen      ← Kalender + Eintrags-Liste
    ├── EintragScreen    ← Erstellen / Bearbeiten / Löschen
    └── EinstellungenScreen ← Export / Import

data/
├── model/TagEintrag     ← Room-Entity
├── db/GueteTagDatabase  ← SQLCipher-verschlüsselt
├── db/TagEintragDao     ← Datenbankzugriffe
└── repository/EintragRepository

security/
├── SecurityManager      ← KeyStore, EncryptedSharedPreferences
└── BiometricHelper      ← Fingerabdruck-Prompt

utils/
├── DateUtil             ← Datumsformatierung
└── ExportImportUtil     ← Ver-/Entschlüsselung, JSON-Serialisierung
```

---

## Abhängigkeiten (wichtigste)

| Bibliothek                       | Version    | Zweck                        |
|----------------------------------|------------|------------------------------|
| AGP                              | 9.2.0      | Android Gradle Plugin        |
| Kotlin                           | 2.1.20     | Sprache                      |
| Compose BOM                      | 2025.05.00 | UI-Framework                 |
| Room                             | 2.7.1      | ORM / Datenbankzugriff       |
| SQLCipher Android                | 4.5.4      | DB-Verschlüsselung           |
| AndroidX Biometric               | 1.4.0-α02  | Fingerabdruck                |
| AndroidX Security Crypto         | 1.1.0-α06  | EncryptedSharedPreferences   |
| Navigation Compose               | 2.9.0      | Screen-Navigation            |
| Gson                             | 2.13.0     | JSON-Serialisierung          |

---

## Sicherheitskonzept

1. **Datenbank**: SQLCipher mit zufällig generiertem Schlüssel (256-Bit AES)  
2. **Schlüsselspeicherung**: `EncryptedSharedPreferences` mit `MasterKey` aus dem Android Keystore  
3. **Biometrie**: `BiometricPrompt` mit `BIOMETRIC_STRONG`; App startet erst nach Authentifizierung  
4. **Export**: AES-256-GCM-Verschlüsselung via Android Keystore; Passwort als IV-Seed  
5. **Export-Passwort**: In `EncryptedSharedPreferences` gespeichert
