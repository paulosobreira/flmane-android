package com.firebaseapp.sowbreira_26fe1.fl_mane;

import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.loopj.android.http.RequestParams;
import com.squareup.picasso.Picasso;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.Date;

import cz.msebera.android.httpclient.Header;
import cz.msebera.android.httpclient.message.BasicHeader;
import jp.wasabeef.picasso.transformations.CropCircleTransformation;


public class LoginActivity extends AppCompatActivity {

    public static final String PREFS_NAME = "F1ManePrefs";
    private static final int RC_SIGN_IN = 1;
    private static final String TAG = "TAG";
    private GoogleSignInClient mGoogleSignInClient;
    private FirebaseAuth mAuth;
    private FirebaseUser user;
    private ProgressDialog progressDialog;
    private View signInButton;
    private View perfil;
    private SharedPreferences settings;

    private int contTentaEntrar = 0;
    private boolean local = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        settings = getSharedPreferences(LoginActivity.PREFS_NAME, 0);
        progressDialog = new ProgressDialog(this);
        carregaHost();
        setContentView(R.layout.activity_login);
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        signInButton = findViewById(R.id.button_google);
        signInButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                signIn();
            }
        });

        mAuth = FirebaseAuth.getInstance();

        user = mAuth.getCurrentUser();

        perfil = findViewById(R.id.perfil);

        if (user != null) {
            preencheFotoNomeUsuario(user);
            signInButton.setVisibility(View.INVISIBLE);
        } else {
            signInButton.setVisibility(View.VISIBLE);
            perfil.setVisibility(View.INVISIBLE);
        }

        perfil.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(LoginActivity.this, PerfilActivity.class);
                startActivity(intent);
                finish();
            }
        });

        findViewById(R.id.srv1).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                contTentaEntrar = 0;
                tentaEntrar();
            }
        });
        findViewById(R.id.exit).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                LoginActivity.this.finishAndRemoveTask();
            }
        });
        TextView versao = findViewById(R.id.versao);
        try {
            PackageInfo pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            versao.setText(pInfo.versionName);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

    }


    private void carregaHost() {
        if(local){
            SharedPreferences.Editor editor = settings.edit();
            editor.putString("host", "http://192.168.15.18:8080");
            editor.commit();
            return;
        }
        new Thread(new Runnable() {

            public void run() {
                URLConnection feedUrl = null;
                try {
                    feedUrl = new URL("https://sowbreira-26fe1.firebaseapp.com/f1mane/host?ver="+new Date().getTime()).openConnection();

                    InputStream is = feedUrl.getInputStream();

                    BufferedReader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
                    String line = null;
                    String host = null;
                    while ((line = reader.readLine()) != null)  {
                        host = line; // add line to list
                    }
                    is.close(); // close input stream
                    SharedPreferences.Editor editor = settings.edit();
                    editor.putString("host", host);
                    editor.commit();
                } catch (IOException e) {
                    Log.e(TAG, "carregaHost: ", e);
                }
            }
        }).start();
    }

    private void tentaEntrar() {
        if (getHost() == null) {
            carregaHost();
        }
        contTentaEntrar++;
        if (user != null) {
            entrarAutenticado();
        } else {
            entrarAnonimo();
        }
    }

    private void entrarAnonimo() {
        String token = settings.getString("token", null);
        progressDialog.setMessage(this.getString(R.string.logando));
        progressDialog.setCancelable(false);
        progressDialog.show();
        AsyncHttpClient client = new AsyncHttpClient();
        String criar = "/f1mane/rest/letsRace/criarSessaoVisitante";
        String renovar = "/f1mane/rest/letsRace/renovarSessaoVisitante/" + token;
        String urlTest = getHost() + (token == null ? criar : renovar);
        client.get(urlTest, new RequestParams(), new JsonHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                Log.d("TAG", "signInWithCredential:success");
                if (progressDialog != null) {
                    progressDialog.dismiss();
                }
                try {
                    JSONObject sessaoCliente = (JSONObject) response.get("sessaoCliente");
                    String token = sessaoCliente.getString("token");
                    SharedPreferences.Editor editor = settings.edit();
                    editor.putString("token", token);
                    editor.commit();
                    irParaMain(token);
                } catch (JSONException e) {
                    Log.e(TAG, "onSuccess: ", e);
                }

            }

            @Override
            public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONObject errorResponse) {
                falhou();
                super.onFailure(statusCode, headers, throwable, errorResponse);
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, String responseString, Throwable throwable) {
                falhou();
                super.onFailure(statusCode, headers, responseString, throwable);
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONArray errorResponse) {
                falhou();
                super.onFailure(statusCode, headers, throwable, errorResponse);
            }
        });
    }

    private String getHost() {
        return settings.getString("host", null);
    }

    private void irParaMain(String token) {
        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
        Bundle extras = new Bundle();
        extras.putString("token", token);
        extras.putString("host", getHost());
        intent.putExtras(extras); //Put your id to your next Intent
        startActivity(intent);
        finish();
    }


    private void preencheFotoNomeUsuario(final FirebaseUser user) {
        String nome = settings.getString("nome", null);
        if(nome == null){
            nome = user.getDisplayName();
            if(nome == null || "".equals(nome)){
                nome = "Lastname";
            }
            SharedPreferences.Editor editor = settings.edit();
            editor.putString("nome", nome);
            editor.commit();
        }

        TextView nomeUsuario = (TextView) findViewById(R.id.nomeUsuario);

        nomeUsuario.setText(nome);

        String foto = settings.getString("foto",null);
        if(foto!=null){
            preenchePortrait(foto);
        }else{
            AsyncHttpClient client = new AsyncHttpClient();
            client.get(user.getPhotoUrl().toString(), new AsyncHttpResponseHandler() {

                @Override
                public void onStart() {
                    // called before request is started
                }

                @Override
                public void onSuccess(int statusCode, Header[] headers, byte[] response) {
                    String foto = user.getPhotoUrl().toString();
                    preenchePortrait(foto);
                }

                @Override
                public void onFailure(int statusCode, Header[] headers, byte[] errorResponse, Throwable e) {
                    String foto = "https://sowbreira-26fe1.firebaseapp.com/f1mane/profile/profile-0.png";
                    preenchePortrait(foto);
                }

                @Override
                public void onRetry(int retryNo) {
                    // called when request is retried
                }
            });
        }
        perfil.setVisibility(View.VISIBLE);
    }

    private void preenchePortrait(String foto) {
        SharedPreferences.Editor editor = settings.edit();
        editor.putString("foto", foto);
        editor.commit();
        ImageView fotoUsuario = (ImageView) findViewById(R.id.fotoUsuario);
        Picasso.with(LoginActivity.this).load(foto)
                .transform(new CropCircleTransformation())
                .placeholder(R.drawable.ic_user_place_holder)
                .into(fotoUsuario);
    }

    private void signIn() {
        Log.w("TAG", "signIn()");
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.w("TAG", "onActivityResult()");
        super.onActivityResult(requestCode, resultCode, data);
        Log.w("TAG", "requestCode =" + requestCode);
        Log.w("TAG", "resultCode =" + resultCode);
        // Result returned from launching the Intent from GoogleSignInClient.getSignInIntent(...);
        if (requestCode == RC_SIGN_IN) {
            // The Task returned from this call is always completed, no need to attach
            // a listener.
            try {
                Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
                GoogleSignInAccount account = task.getResult(ApiException.class);
                firebaseAuthWithGoogle(account);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void firebaseAuthWithGoogle(GoogleSignInAccount acct) {
        Log.d("TAG", "firebaseAuthWithGoogle:" + acct.getId());

        progressDialog.setMessage(this.getString(R.string.logando));
        progressDialog.setCancelable(false);
        progressDialog.show();

        AuthCredential credential = GoogleAuthProvider.getCredential(acct.getIdToken(), null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            user = mAuth.getCurrentUser();
                            preencheFotoNomeUsuario(user);
                            signInButton.setVisibility(View.INVISIBLE);
                        } else {
                            // If sign in fails, display a message to the user.
                            Toast.makeText(LoginActivity.this, task.getException().getLocalizedMessage(),
                                    Toast.LENGTH_LONG).show();
                            Log.w("TAG", "signInWithCredential:failure", task.getException());
                        }
                        if (progressDialog != null) {
                            progressDialog.dismiss();
                        }
                    }
                });
    }

    @NonNull
    private FirebaseUser entrarAutenticado() {
        progressDialog.setMessage(this.getString(R.string.logando));
        progressDialog.setCancelable(false);
        progressDialog.show();

        AsyncHttpClient client = new AsyncHttpClient();
        String url = getHost() + "/f1mane/rest/letsRace/criarSessaoGoogle";

        String nome = settings.getString("nome","Lastname");

        String foto = settings.getString("foto","https://sowbreira-26fe1.firebaseapp.com/f1mane/profile/profile-0.png");

        String email = "";
        if (user.getEmail() != null) {
            email = user.getEmail();
        }

        BasicHeader[] headers = new BasicHeader[]{new BasicHeader("idGoogle", user.getUid()),
                new BasicHeader("nome", nome),
                new BasicHeader("email", email),
                new BasicHeader("urlFoto", foto)};
        client.get(getBaseContext(), url, headers, new RequestParams(), new JsonHttpResponseHandler() {
            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                Log.d("TAG", "signInWithCredential:success");
                if (progressDialog != null) {
                    progressDialog.dismiss();
                }
                try {
                    JSONObject sessaoCliente = (JSONObject) response.get("sessaoCliente");
                    String token = sessaoCliente.getString("token");
                    irParaMain(token);
                } catch (JSONException e) {
                    Log.e(TAG, "onSuccess: ", e);
                }
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONObject errorResponse) {
                falhou();
                super.onFailure(statusCode, headers, throwable, errorResponse);
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, String responseString, Throwable throwable) {
                falhou();
                super.onFailure(statusCode, headers, responseString, throwable);
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONArray errorResponse) {
                falhou();
                super.onFailure(statusCode, headers, throwable, errorResponse);
            }
        });
        return user;
    }

    private void falhou() {
        if (contTentaEntrar < 5) {
            Toast.makeText(LoginActivity.this, getBaseContext().getString(R.string.retry) + " " + contTentaEntrar,
                    Toast.LENGTH_SHORT).show();
            Log.d("TAG", "falhou " + contTentaEntrar);
            tentaEntrar();
        } else {
            if (progressDialog != null) {
                progressDialog.dismiss();
            }
            Toast.makeText(LoginActivity.this, getBaseContext().getString(R.string.server_on_maintenance_come_back_later),
                    Toast.LENGTH_LONG).show();

        }
    }

}
