package com.firebaseapp.sowbreira_26fe1.fl_mane;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "TAG";
    private WebView f1maneView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        String token = null;
        String host = null;

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            token = extras.getString("token");
            Log.d(TAG, "onCreate()token = [" + token + "]");
            host = extras.getString("host");
        }

        String home = "/f1mane/html5/index.html?plataforma=android&limpar=S";


        if (token != null) {
            home += "&token=" + token;
        }

        final String urlSite = host + home;
        iniciaWebView(urlSite);
    }

    private void iniciaWebView(final String urlSite) {
        Handler handler = new Handler();
        final TextView txtView = (TextView) findViewById(R.id.textView);
        Runnable r = new Runnable() {
            public void run() {
                f1maneView = findViewById(R.id.f1mane);
                f1maneView.getSettings().setJavaScriptEnabled(true);
                f1maneView.getSettings().setDomStorageEnabled(true);
                f1maneView.getSettings().setCacheMode(WebSettings.LOAD_DEFAULT);
                f1maneView.setWebViewClient(new WebViewClient());
                f1maneView.addJavascriptInterface(new WebAppInterface(MainActivity.this), "Android");
                f1maneView.loadUrl(urlSite);
                txtView.setVisibility(View.INVISIBLE);

            }
        };
        handler.postDelayed(r, 1000);
    }

    @Override
    public void onBackPressed() {
        Intent intent = new Intent(this, LoginActivity.class);
        this.startActivity(intent);
        this.finish();
        // Otherwise defer to system default behavior.
        super.onBackPressed();
    }
}

