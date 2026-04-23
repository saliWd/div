package ch.widmedia.guetetag.data

import android.content.Context
import android.net.Uri
import ch.widmedia.guetetag.security.SecurityManager
import net.zetetic.database.sqlcipher.SQLiteDatabase
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.channels.FileChannel

class DataPortabilityManager(private val context: Context) {
    private val securityManager = SecurityManager(context)

    /**
     * Exportiert die Datenbank in eine verschlüsselte Datei.
     * @param outputUri Ziel-URI für den Export.
     * @param exportPassword Passwort für die Verschlüsselung der Exportdatei.
     */
    fun exportDatabase(outputUri: Uri, exportPassword: String): Result<Unit> {
        return try {
            // 1. Pfade vorbereiten
            val sourcePath = context.getDatabasePath("guetetag.db").absolutePath
            val tempFile = File(context.cacheDir, "temp_export.db")
            if (tempFile.exists()) tempFile.delete()

            // 2. Interne Passphrase holen
            val internalPassphrase = securityManager.getDatabasePassphrase()

            // 3. Datenbank öffnen und exportieren
            System.loadLibrary("sqlcipher")
            val db = SQLiteDatabase.openOrCreateDatabase(sourcePath, internalPassphrase, null, null, null)
            
            // ATTACH der neuen (temporären) Datenbank mit dem Export-Passwort
            db.rawExecSQL("ATTACH DATABASE '${tempFile.absolutePath}' AS export KEY '$exportPassword';")
            
            // Daten exportieren
            db.rawExecSQL("SELECT sqlcipher_export('export');")
            
            // DETACH und Schließen
            db.rawExecSQL("DETACH DATABASE export;")
            db.close()

            // 4. Temporäre Datei in die Ziel-URI kopieren
            context.contentResolver.openOutputStream(outputUri)?.use { outputStream ->
                FileInputStream(tempFile).use { inputStream ->
                    inputStream.copyTo(outputStream)
                }
            }
            
            tempFile.delete()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Importiert eine verschlüsselte Datenbankdatei.
     * @param inputUri Quell-URI der Importdatei.
     * @param exportPassword Passwort, mit dem die Datei verschlüsselt wurde.
     */
    fun importDatabase(inputUri: Uri, exportPassword: String): Result<Unit> {
        return try {
            // 1. Datei in Cache kopieren
            val tempImportFile = File(context.cacheDir, "temp_import.db")
            context.contentResolver.openInputStream(inputUri)?.use { inputStream ->
                FileOutputStream(tempImportFile).use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }

            // 2. Interne Passphrase holen
            val internalPassphrase = securityManager.getDatabasePassphrase()
            
            // 3. Temporäre Datei für die re-verschlüsselte DB vorbereiten
            val tempInternalFile = File(context.cacheDir, "temp_internal.db")
            if (tempInternalFile.exists()) tempInternalFile.delete()

            // 4. Importierte DB öffnen und mit interner Passphrase neu exportieren
            System.loadLibrary("sqlcipher")
            val db = SQLiteDatabase.openOrCreateDatabase(tempImportFile.absolutePath, exportPassword, null, null, null)
            
            db.rawExecSQL("ATTACH DATABASE '${tempInternalFile.absolutePath}' AS internal KEY '$internalPassphrase';")
            db.rawExecSQL("SELECT sqlcipher_export('internal');")
            db.rawExecSQL("DETACH DATABASE internal;")
            db.close()

            // 5. App-Datenbank schließen und ersetzen
            // WICHTIG: Die Datenbank muss vorher geschlossen sein.
            // In einer realen App sollte man die DB instanz schließen.
            AppDatabase.getDatabase(context).close()
            
            val dbPath = context.getDatabasePath("guetetag.db")
            if (dbPath.exists()) dbPath.delete()
            
            // WAL/SHM Dateien ebenfalls löschen falls vorhanden
            File(dbPath.absolutePath + "-wal").delete()
            File(dbPath.absolutePath + "-shm").delete()

            FileInputStream(tempInternalFile).use { inputStream ->
                FileOutputStream(dbPath).use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }

            // Aufräumen
            tempImportFile.delete()
            tempInternalFile.delete()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
