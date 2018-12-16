package com.firebaseapp.sowbreira_26fe1.fl_mane;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

public class NomeActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final SharedPreferences settings = getSharedPreferences(LoginActivity.PREFS_NAME, 0);
        setContentView(R.layout.activity_nome);
        final EditText editText = findViewById(R.id.txt_nome);
        editText.setText(settings.getString("nome", "Lastname"));
        findViewById(R.id.ok_nome).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                SharedPreferences.Editor editor = settings.edit();
                String nome = editText.getText().toString();
                if (nome != null && !"".equals(nome)) {
                    editor.putString("nome", nome);
                } else {
                    Toast.makeText(NomeActivity.this, "Canceled",
                            Toast.LENGTH_LONG).show();
                }
                editor.commit();
                voltaPerfil();
            }
        });
        findViewById(R.id.cancel_nome).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                NomeActivity.this.finish();
                voltaPerfil();
            }
        });
    }

    private void voltaPerfil() {
        Intent intent = new Intent(NomeActivity.this, PerfilActivity.class);
        startActivity(intent);
        finish();
    }
}
