package com.firebaseapp.sowbreira_26fe1.fl_mane;

import android.content.Intent;
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
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.squareup.picasso.Picasso;

import cz.msebera.android.httpclient.Header;
import jp.wasabeef.picasso.transformations.CropCircleTransformation;

public class PerfilActivity extends AppCompatActivity {
    private View changeNameButton;
    private View signOutButton;
    private View backButton;
    private FirebaseAuth mAuth;
    private FirebaseUser user;
    private GoogleSignInClient mGoogleSignInClient;
    private String nome;
    private String foto;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_perfil);

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
                mudarName();
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

    private void mudarName() {
        Intent intent = new Intent(PerfilActivity.this, NomeActivity.class);
        String nomeExtra = null;
//        if (user != null) {
//            nomeExtra = user.getDisplayName();
//        }
//        if (nome != null && !"".equals(nome)) {
//            nomeExtra = nome;
//        }
        if (nomeExtra != null) {
            Bundle extras = new Bundle();
            extras.putString("nome", nomeExtra);
            intent.putExtras(extras); //Put your id to your next Intent
        }
        startActivity(intent);
        finish();
    }


    private void signOut() {
        mAuth.signOut();
        mGoogleSignInClient.signOut();
        user = null;
        LinearLayout linear = findViewById(R.id.botoes_login);
        linear.removeView(findViewById(new Integer(99)));
        //signOutButton.setVisibility(View.INVISIBLE);
        changeNameButton.setVisibility(View.INVISIBLE);
        removeFotoNomeUsuario();
    }

    private void removeFotoNomeUsuario() {
        TextView nomeUsuario = (TextView) findViewById(R.id.nomeUsuario);
        nomeUsuario.setText(R.string.app_Mane);
        ImageView fotoUsuario = (ImageView) findViewById(R.id.fotoUsuario);
        fotoUsuario.setImageResource(R.drawable.ic_user_place_holder);
    }

    private void preencheFotoNomeUsuario(final FirebaseUser user) {
        TextView nomeUsuario = (TextView) findViewById(R.id.nomeUsuario);
        if (nome == null || "".equals(nome)) {
            nome = user.getDisplayName();
        }
        nomeUsuario.setText(nome);

        AsyncHttpClient client = new AsyncHttpClient();
        client.get(user.getPhotoUrl().toString(), new AsyncHttpResponseHandler() {

            @Override
            public void onStart() {
                // called before request is started
            }

            @Override
            public void onSuccess(int statusCode, Header[] headers, byte[] response) {
                foto = user.getPhotoUrl().toString();
                preenchePortrait();
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, byte[] errorResponse, Throwable e) {
                foto = "https://sowbreira-26fe1.firebaseapp.com/f1mane/profile/profile-0.png";
                preenchePortrait();
            }

            @Override
            public void onRetry(int retryNo) {
                // called when request is retried
            }
        });

    }

    private void preenchePortrait() {
        final ImageView fotoUsuario = (ImageView) findViewById(R.id.fotoUsuario);
        Picasso.with(PerfilActivity.this).load(foto)
                .transform(new CropCircleTransformation())
                .placeholder(R.drawable.ic_user_place_holder)
                .into(fotoUsuario);
    }

}
