package com.firebaseapp.sowbreira_26fe1.fl_mane

import android.app.ProgressDialog
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast

import androidx.appcompat.app.AppCompatActivity
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import androidx.lifecycle.lifecycleScope

import coil3.load
import coil3.request.placeholder
import coil3.request.transformations
import coil3.transform.CircleCropTransformation

import com.firebaseapp.sowbreira_26fe1.fl_mane.databinding.ActivityLoginBinding
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext

import okhttp3.Call
import okhttp3.Callback
import okhttp3.Headers
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response

import org.json.JSONException
import org.json.JSONObject

import java.io.IOException
import java.net.URI
import java.net.URISyntaxException
import java.util.Date

import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var credentialManager: CredentialManager
    private lateinit var mAuth: FirebaseAuth
    private var user: FirebaseUser? = null
    private var progressDialog: ProgressDialog? = null
    private lateinit var settings: SharedPreferences

    private var contTentaEntrar = 0
    // Alterne para true só para testar contra um servidor local; volte para false antes de
    // gerar qualquer build que não seja para uso na sua própria máquina.
    private val local = true
    private val httpClient = OkHttpClient()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        settings = getSharedPreferences(PREFS_NAME, 0)
        progressDialog = ProgressDialog(this)
        carregaHost()
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)
        credentialManager = CredentialManager.create(this)

        binding.buttonGoogle.setOnClickListener {
            signIn()
        }

        mAuth = FirebaseAuth.getInstance()

        user = mAuth.currentUser

        val usuarioAtual = user
        if (usuarioAtual != null) {
            preencheFotoNomeUsuario(usuarioAtual)
            binding.buttonGoogle.visibility = View.INVISIBLE
        } else {
            binding.buttonGoogle.visibility = View.VISIBLE
            binding.perfil.visibility = View.INVISIBLE
        }

        binding.perfil.setOnClickListener {
            startActivity(Intent(this, PerfilActivity::class.java))
            finish()
        }

        binding.srv1.setOnClickListener {
            contTentaEntrar = 0
            tentaEntrar()
        }
        binding.exit.setOnClickListener {
            finishAndRemoveTask()
        }
        try {
            val pInfo = packageManager.getPackageInfo(packageName, 0)
            binding.versao.text = pInfo.versionName
        } catch (e: PackageManager.NameNotFoundException) {
            e.printStackTrace()
        }
    }

    private fun carregaHost() {
        if (local) {
            settings.edit()
                .putString("host", "http://192.168.1.116:8080")
                .putString("appPath", "flmane")
                .commit()
            return
        }
        val request = Request.Builder()
            .url("https://sowbreira-26fe1.firebaseapp.com/f1mane/host?ver=" + Date().time)
            .build()
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                httpClient.newCall(request).await().use { r ->
                    if (!r.isSuccessful) {
                        Log.e(TAG, "carregaHost: HTTP " + r.code)
                        return@launch
                    }
                    val host = r.body.string().trim().lines().last()
                    // Só persiste um host válido (não-vazio, URL https bem formada);
                    // caso contrário mantém o último host conhecido no cache.
                    if (hostValido(host)) {
                        settings.edit()
                            .putString("host", host)
                            .putString("appPath", "f1mane")
                            .commit()
                    } else {
                        Log.w(TAG, "carregaHost: resposta inválida ignorada [$host]")
                    }
                }
            } catch (e: IOException) {
                Log.e(TAG, "carregaHost: ", e)
            }
        }
    }

    private fun hostValido(host: String): Boolean {
        if (host.isBlank()) {
            return false
        }
        return try {
            "https".equals(URI(host).scheme, ignoreCase = true)
        } catch (e: URISyntaxException) {
            false
        }
    }

    private fun tentaEntrar() {
        if (getHost() == null) {
            carregaHost()
        }
        contTentaEntrar++
        if (user != null) {
            entrarAutenticado()
        } else {
            entrarAnonimo()
        }
    }

    private fun entrarAnonimo() {
        val token = settings.getString("token", null)
        mostraProgresso()
        val criar = "/${getAppPath()}/rest/letsRace/criarSessaoVisitante"
        val renovar = "/${getAppPath()}/rest/letsRace/renovarSessaoVisitante/$token"
        val urlTest = getHost() + (if (token == null) criar else renovar)
        val request = Request.Builder().url(urlTest).build()
        lifecycleScope.launch {
            try {
                val body = executa(request)
                if (body == null) {
                    falhou()
                    return@launch
                }
                Log.d(TAG, "signInWithCredential:success")
                progressDialog?.dismiss()
                try {
                    val sessaoCliente = JSONObject(body).getJSONObject("sessaoCliente")
                    val novoToken = sessaoCliente.getString("token")
                    settings.edit().putString("token", novoToken).commit()
                    irParaMain(novoToken)
                } catch (e: JSONException) {
                    Log.e(TAG, "entrarAnonimo: ", e)
                }
            } catch (e: IOException) {
                Log.e(TAG, "entrarAnonimo: ", e)
                falhou()
            }
        }
    }

    private fun entrarAutenticado() {
        val usuarioAtual = user ?: return
        mostraProgresso()

        val url = getHost() + "/${getAppPath()}/rest/letsRace/criarSessaoGoogle"

        val nome = settings.getString("nome", "Lastname")
        val foto = settings.getString("foto", FOTO_PADRAO)
        val email = usuarioAtual.email ?: ""

        // "nome"/"email"/"urlFoto" podem conter bytes não-ASCII (nomes acentuados), que a
        // validação padrão de headers do OkHttp rejeita — o cliente antigo (Apache) permitia.
        val headers = Headers.Builder()
            .add("idGoogle", usuarioAtual.uid)
            .addUnsafeNonAscii("nome", nome!!)
            .addUnsafeNonAscii("email", email)
            .addUnsafeNonAscii("urlFoto", foto!!)
            .build()
        val request = Request.Builder().url(url).headers(headers).build()
        lifecycleScope.launch {
            try {
                val body = executa(request)
                if (body == null) {
                    falhou()
                    return@launch
                }
                Log.d(TAG, "signInWithCredential:success")
                progressDialog?.dismiss()
                try {
                    val sessaoCliente = JSONObject(body).getJSONObject("sessaoCliente")
                    val token = sessaoCliente.getString("token")
                    irParaMain(token)
                } catch (e: JSONException) {
                    Log.e(TAG, "entrarAutenticado: ", e)
                }
            } catch (e: IOException) {
                Log.e(TAG, "entrarAutenticado: ", e)
                falhou()
            }
        }
    }

    private fun getHost(): String? {
        return settings.getString("host", null)
    }

    private fun getAppPath(): String {
        return settings.getString("appPath", "f1mane") ?: "f1mane"
    }

    private fun irParaMain(token: String) {
        val intent = Intent(this, MainActivity::class.java)
        val extras = Bundle()
        extras.putString("token", token)
        extras.putString("host", getHost())
        extras.putString("appPath", getAppPath())
        intent.putExtras(extras)
        startActivity(intent)
        finish()
    }

    private fun preencheFotoNomeUsuario(user: FirebaseUser) {
        var nome = settings.getString("nome", null)
        if (nome == null) {
            nome = user.displayName
            if (nome.isNullOrEmpty()) {
                nome = "Lastname"
            }
            settings.edit().putString("nome", nome).commit()
        }

        binding.nomeUsuario.text = nome

        val foto = settings.getString("foto", null)
        if (foto != null) {
            preenchePortrait(foto)
        } else {
            lifecycleScope.launch {
                val fotoGoogle = user.photoUrl?.toString()
                val existe = if (fotoGoogle == null) {
                    false
                } else {
                    try {
                        withContext(Dispatchers.IO) {
                            httpClient.newCall(Request.Builder().url(fotoGoogle).build())
                                .await().use { it.isSuccessful }
                        }
                    } catch (e: IOException) {
                        false
                    }
                }
                preenchePortrait(if (existe) fotoGoogle!! else FOTO_PADRAO)
            }
        }
        binding.perfil.visibility = View.VISIBLE
    }

    private fun preenchePortrait(foto: String) {
        settings.edit().putString("foto", foto).commit()
        binding.fotoUsuario.load(foto) {
            placeholder(R.drawable.ic_user_place_holder)
            transformations(CircleCropTransformation())
        }
    }

    private fun signIn() {
        Log.w(TAG, "signIn()")
        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(getString(R.string.default_web_client_id))
            .build()
        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()
        lifecycleScope.launch {
            try {
                val result = credentialManager.getCredential(this@LoginActivity, request)
                val credential = result.credential
                if (credential is CustomCredential &&
                    credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
                ) {
                    val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                    firebaseAuthWithGoogle(googleIdTokenCredential.idToken)
                } else {
                    Log.w(TAG, "signIn: credencial inesperada " + credential.type)
                    Toast.makeText(this@LoginActivity, getString(R.string.sign_in_cancelled),
                        Toast.LENGTH_SHORT).show()
                }
            } catch (e: GetCredentialException) {
                Log.w(TAG, "signIn: ", e)
                Toast.makeText(this@LoginActivity, getString(R.string.sign_in_cancelled),
                    Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        Log.d(TAG, "firebaseAuthWithGoogle")

        mostraProgresso()

        val credential = GoogleAuthProvider.getCredential(idToken, null)
        mAuth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    user = mAuth.currentUser
                    user?.let { preencheFotoNomeUsuario(it) }
                    binding.buttonGoogle.visibility = View.INVISIBLE
                } else {
                    // If sign in fails, display a message to the user.
                    Toast.makeText(this, task.exception?.localizedMessage, Toast.LENGTH_LONG).show()
                    Log.w(TAG, "signInWithCredential:failure", task.exception)
                }
                progressDialog?.dismiss()
            }
    }

    private fun falhou() {
        if (contTentaEntrar < 5) {
            Toast.makeText(this, getString(R.string.retry) + " " + contTentaEntrar,
                Toast.LENGTH_SHORT).show()
            Log.d(TAG, "falhou $contTentaEntrar")
            tentaEntrar()
        } else {
            progressDialog?.dismiss()
            Toast.makeText(this, getString(R.string.server_on_maintenance_come_back_later),
                Toast.LENGTH_LONG).show()
        }
    }

    private fun mostraProgresso() {
        progressDialog?.apply {
            setMessage(getString(R.string.logando))
            setCancelable(false)
            show()
        }
    }

    private suspend fun executa(request: Request): String? = withContext(Dispatchers.IO) {
        httpClient.newCall(request).await().use { r ->
            if (!r.isSuccessful) null else r.body.string()
        }
    }

    private suspend fun Call.await(): Response = suspendCancellableCoroutine { cont ->
        enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                cont.resumeWithException(e)
            }

            override fun onResponse(call: Call, response: Response) {
                cont.resume(response)
            }
        })
        cont.invokeOnCancellation { cancel() }
    }

    companion object {
        const val PREFS_NAME = "F1ManePrefs"
        private const val TAG = "TAG"
        private const val FOTO_PADRAO =
            "https://sowbreira-26fe1.firebaseapp.com/f1mane/profile/profile-0.png"
    }
}
