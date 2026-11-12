package com.herotoner.herotv.adb

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.io.File
import java.security.KeyFactory
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.PrivateKey
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import java.security.spec.PKCS8EncodedKeySpec
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

class AdbKeys(val privateKey: PrivateKey, val certificate: X509Certificate)

/**
 * يحفظ مفتاح ADB الخاص مشفّراً: مفتاح AES-256-GCM داخل Android Keystore (لا يخرج من الجهاز)
 * يشفّر ملف المفتاح RSA المحفوظ في مجلد التطبيق الخاص غير المشمول بالنسخ الاحتياطي.
 * إذا تعذّر فكّ التشفير (مثلاً مسح الـ Keystore) يُنشأ مفتاح جديد ويلزم الاقتران من جديد.
 */
class KeyVault(context: Context) {

    private val dir: File = context.noBackupFilesDir
    private val keyFile = File(dir, "adb_key.enc")
    private val certFile = File(dir, "adb_cert.der")

    @Synchronized
    fun loadOrCreate(): AdbKeys {
        if (keyFile.exists() && certFile.exists()) {
            try {
                return load()
            } catch (_: Exception) {
                keyFile.delete()
                certFile.delete()
            }
        }
        return create()
    }

    private fun load(): AdbKeys {
        val blob = keyFile.readBytes()
        require(blob.size > IV_SIZE) { "ملف المفتاح تالف" }
        val iv = blob.copyOfRange(0, IV_SIZE)
        val encrypted = blob.copyOfRange(IV_SIZE, blob.size)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, wrappingKey(), GCMParameterSpec(128, iv))
        val pkcs8 = cipher.doFinal(encrypted)
        val privateKey = KeyFactory.getInstance("RSA").generatePrivate(PKCS8EncodedKeySpec(pkcs8))
        pkcs8.fill(0)
        val certificate = certFile.inputStream().use {
            CertificateFactory.getInstance("X.509").generateCertificate(it) as X509Certificate
        }
        return AdbKeys(privateKey, certificate)
    }

    private fun create(): AdbKeys {
        val generator = KeyPairGenerator.getInstance("RSA")
        generator.initialize(2048)
        val pair = generator.generateKeyPair()
        val certificate = SelfSignedCert.generate(pair, "HERO TV CONTROL", TEN_YEARS_MS)

        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, wrappingKey())
        val pkcs8 = pair.private.encoded
        val encrypted = cipher.doFinal(pkcs8)
        pkcs8.fill(0)
        keyFile.writeBytes(cipher.iv + encrypted)
        certFile.writeBytes(certificate.encoded)
        return AdbKeys(pair.private, certificate)
    }

    private fun wrappingKey(): SecretKey {
        val store = KeyStore.getInstance("AndroidKeyStore")
        store.load(null)
        (store.getKey(ALIAS, null) as? SecretKey)?.let { return it }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        generator.init(
            KeyGenParameterSpec.Builder(
                ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build()
        )
        return generator.generateKey()
    }

    private companion object {
        const val ALIAS = "hero_tv_adb_wrap_key"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val IV_SIZE = 12
        const val TEN_YEARS_MS = 10L * 365L * 24L * 3600L * 1000L
    }
}
