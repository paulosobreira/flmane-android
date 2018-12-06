package com.firebaseapp.sowbreira_26fe1.fl_mane;

import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
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

import cz.msebera.android.httpclient.Header;
import cz.msebera.android.httpclient.message.BasicHeader;
import jp.wasabeef.picasso.transformations.CropCircleTransformation;


public class LoginActivity extends AppCompatActivity {

    public static final String PREFS_NAME = "F1ManePrefs";
    private static final int RC_SIGN_IN = 1;
    private static final String TAG = "TAG";
    private GoogleSignInClient mGoogleSignInClient;
    private FirebaseAuth mAuth;
    private ProgressDialog progressDialog;
    private View signInButton;
    private View signOutButton;
    private View changeNameButton;
    private FirebaseUser user;
    private String nome;
    private int contTentaEntrar = 0;
    private String foto;
    private String host;
    //final String host = "http://192.168.15.17:8080";
    //final String host = "http://j82-sobreira-app.7e14.starter-us-west-2.openshiftapps.com/";
    //String host = "http://f1mane-sobreira.193b.starter-ca-central-1.openshiftapps.com/";
    //final String host = "http://35.198.38.242:80";
    //final String host = "http://192.168.99.100:8080";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        progressDialog = new ProgressDialog(this);
        carregaHost();
        setContentView(R.layout.activity_login);
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);
        signOutButton = findViewById(R.id.sing_out);
        signOutButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                signOut();
            }
        });
        signInButton = findViewById(R.id.button_google);
        signInButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                signIn();
            }
        });

        changeNameButton = findViewById(R.id.change_name);
        changeNameButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mudarName();
            }
        });
        mAuth = FirebaseAuth.getInstance();

        user = mAuth.getCurrentUser();


        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
        String def = "";
        if (user != null) {
            def = user.getDisplayName();
        }
        nome = settings.getString("nome", def);

        if (user != null) {
            preencheFotoNomeUsuario(user);
            criaBotaoLogout();
        } else {
            signInButton.setVisibility(View.VISIBLE);
        }

        findViewById(R.id.fotoUsuarioMore).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mudarFoto();
            }
        });

        findViewById(R.id.srv1).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                contTentaEntrar = 0;
                tentaEntrar();
            }
        });
        /*
        findViewById(R.id.srv2).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //String host = "http://172.17.9.169:80";
                String host = "http://f1mane-sobreira.193b.starter-ca-central-1.openshiftapps.com//";
                if (user != null) {
                    entrarAutenticado(host);
                } else {
                    irParaMain(host, null);
                }
            }
        });
        */
        findViewById(R.id.exit).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                LoginActivity.this.finishAndRemoveTask();
            }
        });
    }

    private void carregaHost() {
        new Thread(new Runnable() {

            public void run() {
                URLConnection feedUrl = null;
                try {
                    feedUrl = new URL("https://sowbreira-26fe1.firebaseapp.com/f1mane/host").openConnection();

                    InputStream is = feedUrl.getInputStream();

                    BufferedReader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
                    String line = null;

                    while ((line = reader.readLine()) != null) // read line by line
                    {
                        host = line; // add line to list
                    }
                    is.close(); // close input stream
                } catch (IOException e) {
                    Log.e(TAG, "carregaHost: ", e);
                }
            }
        }).start();
    }

    private void mudarFoto() {
        Intent intent = new Intent(LoginActivity.this, GridActivity.class);
        startActivity(intent);
        finish();
    }

    private void tentaEntrar() {
        if (host == null) {
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
        final SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
        String token = settings.getString("token", null);
        progressDialog.setMessage(this.getString(R.string.logando));
        progressDialog.setCancelable(false);
        progressDialog.show();
        AsyncHttpClient client = new AsyncHttpClient();
        String criar = "/f1mane/rest/letsRace/criarSessaoVisitante";
        String renovar = "/f1mane/rest/letsRace/renovarSessaoVisitante/" + token;
        String urlTest = host + (token == null ? criar : renovar);
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

    private void mudarName() {
        Intent intent = new Intent(LoginActivity.this, NomeActivity.class);
        String nomeExtra = null;
        if (user != null) {
            nomeExtra = user.getDisplayName();
        }
        if (nome != null && !"".equals(nome)) {
            nomeExtra = nome;
        }
        if (nomeExtra != null) {
            Bundle extras = new Bundle();
            extras.putString("nome", nomeExtra);
            intent.putExtras(extras); //Put your id to your next Intent
        }
        startActivity(intent);
        finish();
    }

    private void irParaMain(String token) {
        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
        Bundle extras = new Bundle();
        extras.putString("token", token);
        extras.putString("host", host);
        intent.putExtras(extras); //Put your id to your next Intent
        startActivity(intent);
        finish();
    }

    private void criaBotaoLogout() {
        signOutButton.setVisibility(View.VISIBLE);
        changeNameButton.setVisibility(View.VISIBLE);
        signInButton.setVisibility(View.INVISIBLE);
    }

    private void signOut() {
        mAuth.signOut();
        mGoogleSignInClient.signOut();
        user = null;
        LinearLayout linear = findViewById(R.id.botoes_login);
        linear.removeView(findViewById(new Integer(99)));
        signOutButton.setVisibility(View.INVISIBLE);
        changeNameButton.setVisibility(View.INVISIBLE);
        signInButton.setVisibility(View.VISIBLE);
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
                foto = "https://sowbreira-26fe1.firebaseapp.com/f1mane/headset.png";
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
                            criaBotaoLogout();
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
        String url = host + "/f1mane/rest/letsRace/criarSessaoGoogle";


        if ((nome == null || "".equals(nome)) && user.getDisplayName() != null) {
            nome = user.getDisplayName();
        }
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
