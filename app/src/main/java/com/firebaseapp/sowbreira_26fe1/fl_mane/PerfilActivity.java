package com.firebaseapp.sowbreira_26fe1.fl_mane;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.squareup.picasso.Picasso;

import jp.wasabeef.picasso.transformations.CropCircleTransformation;

public class PerfilActivity extends AppCompatActivity {
    private View changeNameButton;
    private View changePictureButton;
    private View signOutButton;
    private View backButton;
    private FirebaseAuth mAuth;
    private FirebaseUser user;
    private GoogleSignInClient mGoogleSignInClient;
    private String nome;
    private String foto;
    private SharedPreferences settings;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_perfil);
        settings = getSharedPreferences(LoginActivity.PREFS_NAME, 0);
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);
        mAuth = FirebaseAuth.getInstance();

        user = mAuth.getCurrentUser();

        changeNameButton = findViewById(R.id.change_name);
        changeNameButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(PerfilActivity.this, NomeActivity.class);
                startActivity(intent);
                finish();
            }
        });

        changePictureButton = findViewById(R.id.change_picture);
        changePictureButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(PerfilActivity.this, GridActivity.class);
                startActivity(intent);
                finish();
            }
        });

        signOutButton = findViewById(R.id.sing_out);
        signOutButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                signOut();
                voltar();
            }
        });
        backButton= findViewById(R.id.voltar);
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                voltar();
            }
        });
        if (user != null) {
            preencheFotoNomeUsuario(user);
            signOutButton.setVisibility(View.VISIBLE);
            changeNameButton.setVisibility(View.VISIBLE);
        }
    }

    private void voltar() {
        Intent intent = new Intent(PerfilActivity.this, LoginActivity.class);
        startActivity(intent);
        finish();
    }


    private void signOut() {
        mAuth.signOut();
        mGoogleSignInClient.signOut();
        user = null;
        LinearLayout linear = findViewById(R.id.botoes_login);
        linear.removeView(findViewById(new Integer(99)));
        settings.edit().clear();
    }


    private void preencheFotoNomeUsuario(final FirebaseUser user) {
        TextView nomeUsuario = (TextView) findViewById(R.id.nomeUsuario);
        String nome = settings.getString("nome", null);
        nomeUsuario.setText(nome);
        String foto = settings.getString("foto", null);
        ImageView fotoUsuario = (ImageView) findViewById(R.id.fotoUsuario);
        Picasso.with(PerfilActivity.this).load(foto)
                .transform(new CropCircleTransformation())
                .placeholder(R.drawable.ic_user_place_holder)
                .into(fotoUsuario);

    }

}
