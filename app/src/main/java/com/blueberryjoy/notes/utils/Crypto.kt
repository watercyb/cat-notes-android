package com.blueberryjoy.notes.utils

import android.util.Base64
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

object Crypto {
    private const val SALT_SIZE = 16
    private const val IV_SIZE = 12
    private const val ITERATIONS = 200000
    private const val KEY_LENGTH = 256

    fun encrypt(password: String, plaintext: String): String {

        val random = SecureRandom()

        val salt = ByteArray(SALT_SIZE)
        random.nextBytes(salt)

        val iv = ByteArray(IV_SIZE)
        random.nextBytes(iv)

        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")

        val spec = PBEKeySpec(
            password.toCharArray(),
            salt,
            ITERATIONS,
            KEY_LENGTH
        )

        val keyBytes = factory.generateSecret(spec).encoded
        val key = SecretKeySpec(keyBytes, "AES")

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")

        val gcmSpec = GCMParameterSpec(128, iv)

        cipher.init(Cipher.ENCRYPT_MODE, key, gcmSpec)

        val ciphertext = cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))

        val combined = ByteArray(salt.size + iv.size + ciphertext.size)

        System.arraycopy(salt, 0, combined, 0, salt.size)
        System.arraycopy(iv, 0, combined, salt.size, iv.size)
        System.arraycopy(ciphertext, 0, combined, salt.size + iv.size, ciphertext.size)

        return Base64.encodeToString(combined, Base64.NO_WRAP)
    }

    fun decrypt(password: String, encrypted: String): String {

        val combined = Base64.decode(encrypted, Base64.NO_WRAP)

        val salt = combined.copyOfRange(0, 16)
        val iv = combined.copyOfRange(16, 28)
        val ciphertext = combined.copyOfRange(28, combined.size)

        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")

        val spec = PBEKeySpec(
            password.toCharArray(),
            salt,
            ITERATIONS,
            KEY_LENGTH
        )

        val keyBytes = factory.generateSecret(spec).encoded
        val key = SecretKeySpec(keyBytes, "AES")

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")

        val gcmSpec = GCMParameterSpec(128, iv)

        cipher.init(Cipher.DECRYPT_MODE, key, gcmSpec)

        val plaintextBytes = cipher.doFinal(ciphertext)

        return String(plaintextBytes, Charsets.UTF_8)
    }
}