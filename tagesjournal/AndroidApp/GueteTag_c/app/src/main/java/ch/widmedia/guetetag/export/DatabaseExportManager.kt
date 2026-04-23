
package ch.widmedia.guetetag.export
import android.content.Context
import java.io.File
object DatabaseExportManager {
    fun export(context: Context, password: ByteArray, target: File) {
        val data = context.getDatabasePath("guetetag.db").readBytes()
        target.writeBytes(CryptoUtil.encrypt(data, password))
    }
    fun import(context: Context, password: ByteArray, source: File) {
        val decrypted = CryptoUtil.decrypt(source.readBytes(), password)
        context.getDatabasePath("guetetag.db").writeBytes(decrypted)
    }
}
