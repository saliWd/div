
package ch.widmedia.guetetag.export
import java.security.SecureRandom
import javax.crypto.*
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec
object CryptoUtil {
    fun encrypt(data: ByteArray, password: ByteArray): ByteArray {
        val iv = ByteArray(12).also { SecureRandom().nextBytes(it) }
        val c = Cipher.getInstance("AES/GCM/NoPadding")
        c.init(Cipher.ENCRYPT_MODE, SecretKeySpec(password, "AES"), GCMParameterSpec(128, iv))
        return iv + c.doFinal(data)
    }
    fun decrypt(data: ByteArray, password: ByteArray): ByteArray {
        val iv = data.copyOfRange(0,12)
        val c = Cipher.getInstance("AES/GCM/NoPadding")
        c.init(Cipher.DECRYPT_MODE, SecretKeySpec(password, "AES"), GCMParameterSpec(128, iv))
        return c.doFinal(data.copyOfRange(12, data.size))
    }
}
