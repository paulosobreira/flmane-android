package com.firebaseapp.sowbreira_26fe1.fl_mane

import android.content.Intent
import android.os.Bundle

import androidx.appcompat.app.AppCompatActivity

import com.firebaseapp.sowbreira_26fe1.fl_mane.databinding.ActivityGridBinding

class GridActivity : AppCompatActivity() {

    private var itemPosition = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityGridBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.gridview.adapter = ImageAdapter(this)

        binding.gridview.setOnItemClickListener { _, _, position, _ ->
            binding.gridview.setItemChecked(position, true)
            itemPosition = position
        }

        binding.okFoto.setOnClickListener {
            if (itemPosition < 0) {
                return@setOnClickListener
            }
            val settings = getSharedPreferences(LoginActivity.PREFS_NAME, 0)
            settings.edit()
                .putString("foto", "https://sowbreira-26fe1.firebaseapp.com/f1mane/profile/profile-$itemPosition.png")
                .commit()
            voltaPerfil()
        }
        binding.cancelFoto.setOnClickListener {
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
