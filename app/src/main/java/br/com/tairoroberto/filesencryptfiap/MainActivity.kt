package br.com.tairoroberto.filesencryptfiap

import android.app.KeyguardManager
import android.content.SharedPreferences
import android.os.Build
import android.os.Bundle
import android.security.KeyPairGeneratorSpec
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.security.keystore.UserNotAuthenticatedException
import android.support.v7.app.AppCompatActivity
import android.util.Base64
import android.util.Log
import android.view.View
import android.widget.EditText
import android.widget.Toast
import kotlinx.android.synthetic.main.activity_main.*
import java.io.IOException
import java.math.BigInteger
import java.security.*
import java.security.cert.CertificateException
import java.util.*
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.IvParameterSpec
import javax.security.auth.x500.X500Principal

class MainActivity : AppCompatActivity() {

    val TAG = "SimpleKeystoreApp"
    public val AUTHENTICATION_DURATION_SECONDS = 30
    public val KEY_ALIAS = "MY_KEY_ALIAS"

    private val CHARSET_NAME = Charsets.UTF_8
    private val ANDROID_KEY_STORE = "AndroidKeyStore"
    private val TRANSFORMATION = KeyProperties.KEY_ALGORITHM_AES + "/" + KeyProperties.BLOCK_MODE_CBC + "/" + KeyProperties.ENCRYPTION_PADDING_PKCS7
    private val keyguardManager: KeyguardManager? = null
    private val SAVE_CREDENTIALS_REQUEST_CODE = 1

    val generator: KeyPairGenerator = KeyPairGenerator.getInstance("RSA", "AndroidKeyStore")
    var keyPair: KeyPair? = null
    var keyStore: KeyStore? = null

    var tvOriginalText: EditText? = null
    var decryptedText: EditText? = null
    var encryptedText: EditText? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)

        tvOriginalText = findViewById(R.id.tvOriginalText)
        decryptedText = findViewById(R.id.decryptedText)
        encryptedText = findViewById(R.id.encryptedText)

        try {
            keyStore = KeyStore.getInstance("AndroidKeyStore")
            keyStore?.load(null)
        } catch (e: Exception) {
        }
    }

    fun createNewKeys(view: View) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            createKeyAnndroidGreaterThanM()
            setEncryptedData(tvOriginalText?.text.toString())
        } else {
            createKeyAndroiLessThanM()
        }
    }


    fun createKeyAndroiLessThanM() {
        try {
            // Create new key if needed
            if (keyStore?.containsAlias(KEY_ALIAS) == false) {
                val start: Calendar = Calendar.getInstance()
                val end: Calendar = Calendar.getInstance()
                end.add(Calendar.YEAR, 1)
                val spec: KeyPairGeneratorSpec = KeyPairGeneratorSpec.Builder(this)
                        .setAlias(KEY_ALIAS)
                        .setSubject(X500Principal("CN=Sample Name, O=Android Authority"))
                        .setSerialNumber(BigInteger.ONE)
                        .setStartDate(start.time)
                        .setEndDate(end.time)
                        .build()
                generator.initialize(spec)

                keyPair = generator.generateKeyPair()
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Exception ${e.message} occured", Toast.LENGTH_LONG).show()
            Log.e(TAG, Log.getStackTraceString(e))
        }
    }


    /**
     * Creates a symmetric key in the Android Key Store which can only be used after the user has
     * authenticated with device credentials within the last X seconds.
     */
    private fun createKeyAnndroidGreaterThanM() {
        try {
            val keyStore = KeyStore.getInstance("AndroidKeyStore")
            keyStore.load(null)

            // Para gerar a chave de segurança
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

                generator.initialize(KeyGenParameterSpec.Builder(KEY_ALIAS, KeyProperties.PURPOSE_DECRYPT)
                        .setDigests(KeyProperties.DIGEST_SHA256, KeyProperties.DIGEST_SHA512)
                        .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_RSA_OAEP)
                        // Requerida a autenticação de dispositivo função. Função de autenticação de dispositivos é OFF excepção de segurança ocorre
                        .setUserAuthenticationRequired(true)
                        .build())
                keyPair = generator.generateKeyPair()
                val cipher: Cipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding")
                cipher.init(Cipher.DECRYPT_MODE, keyPair?.private)
            }

        } catch (e: InvalidAlgorithmParameterException) {
            throw RuntimeException("Ele não conseguiu gerar a chave\n", e)
        } catch (e: CertificateException) {
            throw RuntimeException("Ele não conseguiu gerar a chave\n", e)
        } catch (e: NoSuchAlgorithmException) {
            throw RuntimeException("Ele não conseguiu gerar a chave\n", e)
        } catch (e: KeyStoreException) {
            throw RuntimeException("Ele não conseguiu gerar a chave\n", e)
        } catch (e: IOException) {
            throw RuntimeException("Ele não conseguiu gerar a chave\n", e)
        } catch (e: NoSuchProviderException) {
            throw RuntimeException("Ele não conseguiu gerar a chave\n", e)
        }
    }

    private fun createKey(): SecretKey? {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEY_STORE)

                keyGenerator.init(KeyGenParameterSpec.Builder(KEY_ALIAS,
                        KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                        .setBlockModes(KeyProperties.BLOCK_MODE_CBC)
                        //.setUserAuthenticationRequired(true)
                        .setUserAuthenticationValidityDurationSeconds(AUTHENTICATION_DURATION_SECONDS)
                        .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_PKCS7)
                        .build())

                return keyGenerator.generateKey()
            } catch (e: NoSuchAlgorithmException) {
                throw RuntimeException("Failed to create a symmetric key", e)
            } catch (e: NoSuchProviderException) {
                throw RuntimeException("Failed to create a symmetric key", e)
            } catch (e: InvalidAlgorithmParameterException) {
                throw RuntimeException("Failed to create a symmetric key", e)
            }
        }
        return null
    }

    fun setEncryptedData(data: String) {
        // encrypt the password
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                val secretKey: SecretKey? = createKey()
                val cipher: Cipher = Cipher.getInstance(TRANSFORMATION)
                cipher.init(Cipher.ENCRYPT_MODE, secretKey)
                val encryptionIv = cipher.iv
                val passwordBytes = data.toByteArray(CHARSET_NAME)
                val encryptedPasswordBytes = cipher.doFinal(passwordBytes)
                val encryptedPassword: String = Base64.encodeToString(encryptedPasswordBytes, Base64.DEFAULT)

                // store the login data in the shared preferences
                // only the password is encrypted, IV used for the encryption is stored
                val sharedPreferences: SharedPreferences = this.getPreferences(android.content.Context.MODE_PRIVATE)
                val editor: SharedPreferences.Editor = sharedPreferences.edit()
                editor.putString("originalText", encryptedPassword)
                editor.putString("encryptionIv", Base64.encodeToString(encryptionIv, Base64.DEFAULT))
                editor.apply()
                encryptedText?.setText(encryptedPassword)

            } catch (e: UserNotAuthenticatedException) {
                e.printStackTrace()
                showAuthenticationScreen(SAVE_CREDENTIALS_REQUEST_CODE)
            }
        }
    }


    fun getDecryptedData(view: View) {
        // load login data from shared preferences (
        // only the password is encrypted, IV used for the encryption is loaded from shared preferences
        val sharedPreferences: SharedPreferences = this.getPreferences(android.content.Context.MODE_PRIVATE)

        val base64EncryptedPassword: String = sharedPreferences.getString("originalText", null)
        val base64EncryptionIv: String = sharedPreferences.getString("encryptionIv", null)
        val encryptionIv = Base64.decode(base64EncryptionIv, Base64.DEFAULT)
        val encryptedPassword = Base64.decode(base64EncryptedPassword, Base64.DEFAULT)

        // decrypt the password
        val keyStore: KeyStore = KeyStore.getInstance(ANDROID_KEY_STORE)
        keyStore.load(null)
        val secretKey: SecretKey = keyStore.getKey(KEY_ALIAS, null) as SecretKey
        val cipher: Cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, secretKey, IvParameterSpec(encryptionIv))
        val passwordBytes = cipher.doFinal(encryptedPassword)

        val string = kotlin.text.String(passwordBytes, CHARSET_NAME)

        decryptedText?.setText(string)
    }

    fun saveDataWithoutEncrypt(view: View){
        val sharedPreferences: SharedPreferences = this.getPreferences(android.content.Context.MODE_PRIVATE)
        val editor: SharedPreferences.Editor = sharedPreferences.edit()
        editor.putString("originalText", tvOriginalText?.text.toString())
        editor.apply()
    }

    private fun showAuthenticationScreen(requestCode: Int) {
        val intent = keyguardManager?.createConfirmDeviceCredentialIntent(null, null)
        if (intent != null) {
            startActivityForResult(intent, requestCode)
        }
    }
}