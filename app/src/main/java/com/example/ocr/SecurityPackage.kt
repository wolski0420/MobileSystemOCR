package com.example.ocr

import android.util.Log
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.spec.PBEKeySpec
import javax.crypto.SecretKeyFactory
import com.macasaet.fernet.Key
import com.macasaet.fernet.Token
import com.macasaet.fernet.StringValidator
import com.macasaet.fernet.Validator
import java.time.Duration
import java.time.temporal.TemporalAmount
import java.security.MessageDigest



class SecurityPackage {
    // Data from encryption
    private fun deriveKey(password: String, salt: ByteArray): String {
        val iterations = 100000
        val derivedKeyLength = 256
        val spec = PBEKeySpec(password.toCharArray(), salt, iterations, derivedKeyLength)
        val secretKeyFactory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val key = secretKeyFactory.generateSecret(spec).encoded
        return Base64.getUrlEncoder().encodeToString(key)
    }

    public fun prepareKey(): Key {
        val salt = "@TXu,rbr* x\u000c*Cpm".toByteArray()
        val key = deriveKey("my password", salt)
        val fernetKey = Key(key)
        return fernetKey
    }

    public fun decrypt(textin: String): String {
        val validator: Validator<String> = object : StringValidator {
            override fun getTimeToLive(): TemporalAmount {
                return Duration.ofHours(1)
            }
        }
        val textin1 = textin.substring(1, textin.length -1)
        Log.d("MainActivity - CloudOCR", textin1)
        var token = Token.fromString(textin1)
        val data = token.validateAndDecrypt(prepareKey(), validator)
        return data
    }

    public fun encrpt(data: ByteArray): ByteArray {
        val token = Token.generate(SecureRandom(), prepareKey(), data)
        return token.serialise().toByteArray()
    }

    public fun encrpt(data: String): String {
        val token = Token.generate(SecureRandom(), prepareKey(), data)
        return token.serialise()
    }

    private fun hashString(type: String, input: ByteArray): ByteArray {
        val bytes = MessageDigest
            .getInstance(type)
            .digest(input)
        return bytes
    }

    public fun sha256(input: ByteArray) = hashString("SHA-256", input)


    }

