package com.firebaseapp.sowbreira_26fe1.fl_mane

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log

import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CredentialManagerCallback
import androidx.credentials.exceptions.ClearCredentialException

import coil3.load
import coil3.request.placeholder
import coil3.request.transformations
import coil3.transform.CircleCropTransformation

import com.firebaseapp.sowbreira_26fe1.fl_mane.databinding.ActivityPerfilBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser

class PerfilActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPerfilBinding
    private lateinit var mAuth: FirebaseAuth
    private var user: FirebaseUser? = null
    private lateinit var credentialManager: CredentialManager
    private lateinit var settings: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPerfilBinding.inflate(layoutInflater)
        setContentView(binding.root)
        settings = getSharedPreferences(LoginActivity.PREFS_NAME, 0)
        credentialManager = CredentialManager.create(this)
        mAuth = FirebaseAuth.getInstance()

        user = mAuth.currentUser

        binding.changeName.setOnClickListener {
            startActivity(Intent(this, NomeActivity::class.java))
            finish()
        }

        binding.changePicture.setOnClickListener {
            startActivity(Intent(this, GridActivity::class.java))
            finish()
        }

        binding.singOut.setOnClickListener {
            signOut()
            voltar()
        }
        binding.voltar.setOnClickListener {
            voltar()
        }
        if (user != null) {
            preencheFotoNomeUsuario()
        }
    }

    private fun voltar() {
        startActivity(Intent(this, LoginActivity::class.java))
        finish()
    }

    private fun signOut() {
        mAuth.signOut()
        credentialManager.clearCredentialStateAsync(ClearCredentialStateRequest(), null,
            ContextCompat.getMainExecutor(this),
            object : CredentialManagerCallback<Void?, ClearCredentialException> {
                override fun onResult(result: Void?) {
                }

                override fun onError(e: ClearCredentialException) {
                    Log.w(TAG, "signOut: ", e)
                }
            })
        user = null
        settings.edit().clear().commit()
    }

    private fun preencheFotoNomeUsuario() {
        binding.nomeUsuario.text = settings.getString("nome", null)
        val foto = settings.getString("foto", null)
        if (foto != null) {
            binding.fotoUsuario.load(foto) {
                placeholder(R.drawable.ic_user_place_holder)
                transformations(CircleCropTransformation())
            }
        }
    }

    companion object {
        private const val TAG = "TAG"
    }
}
