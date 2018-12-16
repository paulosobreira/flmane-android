package com.firebaseapp.sowbreira_26fe1.fl_mane;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.GridView;

public class GridActivity extends AppCompatActivity {

    private int itemPosition = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_grid);
        final GridView gridview = (GridView) findViewById(R.id.gridview);
        gridview.setAdapter(new ImageAdapter(this));

        gridview.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View v,
                                    int position, long id) {
                gridview.setItemChecked(position, true);
                itemPosition = position;
            }
        });

        findViewById(R.id.ok_foto).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(itemPosition<0){
                    return;
                }
                SharedPreferences settings = getSharedPreferences(LoginActivity.PREFS_NAME, 0);
                SharedPreferences.Editor editor = settings.edit();
                editor.putString("foto", "https://sowbreira-26fe1.firebaseapp.com/f1mane/profile/profile-"+itemPosition+".png");
                editor.commit();
                voltaPerfil();
            }
        });
        findViewById(R.id.cancel_foto).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                GridActivity.this.finish();
                voltaPerfil();
            }
        });

    }

    private void voltaPerfil() {
        Intent intent = new Intent(GridActivity.this, PerfilActivity.class);
        startActivity(intent);
        finish();
    }

}
