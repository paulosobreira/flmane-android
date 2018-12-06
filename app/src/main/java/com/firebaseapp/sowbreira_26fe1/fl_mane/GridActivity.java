package com.firebaseapp.sowbreira_26fe1.fl_mane;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.Toast;

public class GridActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_grid);
        GridView gridview = (GridView) findViewById(R.id.gridview);
        gridview.setAdapter(new ImageAdapter(this));

        gridview.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View v,
                                    int position, long id) {
                Toast.makeText(GridActivity.this, "" + position,
                        Toast.LENGTH_SHORT).show();
            }
        });

        findViewById(R.id.ok_foto).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                SharedPreferences settings = getSharedPreferences(LoginActivity.PREFS_NAME, 0);
                SharedPreferences.Editor editor = settings.edit();
//                String nome = editText.getText().toString();
//                if (nome != null && !"".equals(nome)) {
//                    editor.putString("nome", nome);
//                } else {
//                    Toast.makeText(NomeActivity.this, "Canceled",
//                            Toast.LENGTH_LONG).show();
//                }
                editor.commit();
                voltaLogin();
            }
        });
        findViewById(R.id.cancel_foto).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                GridActivity.this.finish();
                voltaLogin();
            }
        });

    }

    private void voltaLogin() {
        Intent intent = new Intent(GridActivity.this, LoginActivity.class);
        startActivity(intent);
        finish();
    }

}
