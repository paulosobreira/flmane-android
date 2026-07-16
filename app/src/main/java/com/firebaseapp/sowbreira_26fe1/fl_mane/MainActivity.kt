package com.firebaseapp.sowbreira_26fe1.fl_mane

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.webkit.WebSettings
import android.webkit.WebViewClient

import androidx.appcompat.app.AppCompatActivity

import com.firebaseapp.sowbreira_26fe1.fl_mane.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        var token: String? = null
        var host: String? = null
        var appPath = "f1mane"

        intent.extras?.let { extras ->
            token = extras.getString("token")
            Log.d(TAG, "onCreate()token = [$token]")
            host = extras.getString("host")
            appPath = extras.getString("appPath") ?: "f1mane"
        }

        var home = "/$appPath/html5/index.html?plataforma=android&limpar=S"

        if (token != null) {
            home += "&token=$token"
        }

        iniciaWebView(host + home)
    }

    private fun iniciaWebView(urlSite: String) {
        Handler(Looper.getMainLooper()).postDelayed({
            binding.f1mane.settings.javaScriptEnabled = true
            binding.f1mane.settings.domStorageEnabled = true
            binding.f1mane.settings.cacheMode = WebSettings.LOAD_DEFAULT
            binding.f1mane.webViewClient = WebViewClient()
            binding.f1mane.addJavascriptInterface(WebAppInterface(this), "Android")
            binding.f1mane.loadUrl(urlSite)
            binding.textView.visibility = View.INVISIBLE
        }, 1000)
    }

    override fun onBackPressed() {
        val intent = Intent(this, LoginActivity::class.java)
        startActivity(intent)
        finish()
        // Otherwise defer to system default behavior.
        super.onBackPressed()
    }

    companion object {
        private const val TAG = "TAG"
    }
}
