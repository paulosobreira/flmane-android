package com.firebaseapp.sowbreira_26fe1.fl_mane

import android.content.Intent
import android.os.Bundle
import android.widget.Toast

import androidx.appcompat.app.AppCompatActivity

import com.firebaseapp.sowbreira_26fe1.fl_mane.databinding.ActivityNomeBinding

class NomeActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val settings = getSharedPreferences(LoginActivity.PREFS_NAME, 0)
        val binding = ActivityNomeBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.txtNome.setText(settings.getString("nome", "Lastname"))
        binding.okNome.setOnClickListener {
            val editor = settings.edit()
            val nome = binding.txtNome.text.toString()
            if (nome.isNotEmpty()) {
                editor.putString("nome", nome)
            } else {
                Toast.makeText(this, "Canceled", Toast.LENGTH_LONG).show()
            }
            editor.commit()
            voltaPerfil()
        }
        binding.cancelNome.setOnClickListener {
            finish()
            voltaPerfil()
        }
    }

    private fun voltaPerfil() {
        val intent = Intent(this, PerfilActivity::class.java)
        startActivity(intent)
        finish()
    }
}
