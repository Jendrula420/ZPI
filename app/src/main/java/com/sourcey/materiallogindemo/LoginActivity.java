package com.sourcey.materiallogindemo;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import butterknife.BindView;
import butterknife.ButterKnife;

public class LoginActivity extends AppCompatActivity {
    private static final String TAG = "LoginActivity";
    private static final int REQUEST_SIGNUP = 0;
    private FirebaseAuth firebaseAuth;
    private boolean isRegistered;

    @BindView(R.id.input_email) EditText _emailText;
    @BindView(R.id.input_password) EditText _passwordText;
    @BindView(R.id.btn_login) Button _loginButton;
    @BindView(R.id.link_signup) TextView _signupLink;
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        ButterKnife.bind(this);

        firebaseAuth = FirebaseAuth.getInstance();

        if(firebaseAuth.getCurrentUser() != null)
            firebaseAuth.signOut();
        
        _loginButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v)
            {
                if(validate())
                {
                    login();
                }
            }
        });

        _signupLink.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                Intent intent = new Intent(getApplicationContext(), SignupActivity.class);
                startActivityForResult(intent, REQUEST_SIGNUP);
                finish();
                overridePendingTransition(R.anim.push_left_in, R.anim.push_left_out);
            }
        });

        loadUser();
    }

    public void login() {
        Log.d(TAG, getString(R.string.login));
        _loginButton.setEnabled(false);

        final ProgressDialog progressDialog = new ProgressDialog(LoginActivity.this, R.style.AppTheme_Dark_Dialog);
        progressDialog.setIndeterminate(true);
        progressDialog.setMessage(getString(R.string.login));
        progressDialog.show();

        final String email = _emailText.getText().toString().trim();
        String password = _passwordText.getText().toString().trim();

        firebaseAuth.signInWithEmailAndPassword(email, password).addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {
                progressDialog.dismiss();
                if(task.isSuccessful()){
                    Intent intent= null;
                    if(email.matches("^[0-9]+(@student.pwr.edu.pl)"))
                        intent = new Intent(getApplicationContext(), PresenceActivity.class);
                    else if(email.matches("^[a-z]+(.)[a-z-]+(@pwr.edu.pl)"))
                        //change
                        intent = new Intent(getApplicationContext(), GenerateKeyActivity.class);
                    if(intent != null)
                    {
                        if(!isRegistered)
                            saveUser();
                        startActivityForResult(intent, REQUEST_SIGNUP);
                        finish();
                        overridePendingTransition(R.anim.push_left_in, R.anim.push_left_out);
                    }
                    else {
                        Toast.makeText(getBaseContext(), getString(R.string.login_failed), Toast.LENGTH_LONG).show();
                        _loginButton.setEnabled(true);
                    }
                }
                else{
                    Toast.makeText(getBaseContext(), getString(R.string.login_failed), Toast.LENGTH_LONG).show();
                    _loginButton.setEnabled(true);
                }
            }
        });
    }



    public boolean validate()
    {
        boolean valid = true;
        //*
        String email = _emailText.getText().toString();
        String password = _passwordText.getText().toString();
        if(!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches() )
        {
            if (email.length() == 6 && email.matches("^[0-9]*$"))
            {
                email += "@student.pwr.edu.pl";
            } else if (email.matches("[a-z]+[.][a-z-.]*$"))
            {
                email += "@pwr.edu.pl";
            }
            else
            {
                _emailText.setError(getString(R.string.e_mail_error));
                valid = false;
            }
        }
        if(valid)
        {
            _emailText.setText(email);
            _passwordText.setError(null);
        }
        if (password.isEmpty() || password.length() < 6)
        {
            _passwordText.setError(getString(R.string.password_error));
            valid = false;
        } else
        {
            _passwordText.setError(null);
        }

        if(!isInternetConnection())
        {
            if(isRegistered)
            {
                //TODO sprawdzić zgodnosć emial i password z plikiem
                offlineDialog();
            }
            else
            {
                internetConnectionDialog();
            }
            valid = false;
        }
        //*/
        return valid;
    }

    public String getDeviceID()
    {
        //TODO Opcjonalnie dodać identyfikator urządzenia  w celu zabezpiecznia przed oszustwami
        String deviceUniqueIdentifier = null;
        TelephonyManager tm = (TelephonyManager) this.getSystemService(Context.TELEPHONY_SERVICE);
        if (null != tm)
        {
            try
            {
                deviceUniqueIdentifier = tm.getDeviceId();
            }
            catch (SecurityException e) {}
        }
        if (null == deviceUniqueIdentifier || 0 == deviceUniqueIdentifier.length()) {
            deviceUniqueIdentifier = Settings.Secure.getString(this.getContentResolver(), Settings.Secure.ANDROID_ID);
        }
        return deviceUniqueIdentifier;
    }

    public boolean isInternetConnection()
    {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        try
        {
            NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
            return activeNetworkInfo != null && activeNetworkInfo.isConnected();
        } catch (NullPointerException e) {return false;}
    }

    public void internetConnectionDialog()
    {
        if(!isInternetConnection())
        {
            final ProgressDialog progressDialog = new ProgressDialog(LoginActivity.this, R.style.AppTheme_Dark_Dialog);
            progressDialog.setIndeterminate(true);
            progressDialog.setMessage(getString(R.string.internet_connection_error));
            progressDialog.show();
            //TODO internet_connection_listner
            /*
            boolean valid = false;
            while (!valid) {
                if (checkInternetConnection()) {
                    valid = true;
                    progressDialog.dismiss();
                } else
                    try {
                        Thread.sleep(3000);
                    } catch (InterruptedException e) { e.printStackTrace(); }
            }
            //*/
        }
    }


    public void saveUser()
    {
        //TODO zapis danych urzytkownika z bazy danych i kopatybilna metoda odczytu
        //TODO szyfrowanie danych
        //Toast.makeText(getApplicationContext(), "u.zapis", Toast.LENGTH_LONG).show();
        try {
            FileOutputStream os = openFileOutput("user", Context.MODE_PRIVATE);
            DataOutputStream dos = new DataOutputStream(new BufferedOutputStream(os));

            dos.writeUTF(_emailText.getText().toString());
            dos.writeUTF(_passwordText.getText().toString());
            dos.close();
            //Toast.makeText(getApplicationContext(), "u.zapisano", Toast.LENGTH_LONG).show();
        } catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    public void loadUser()
    {
        //Toast.makeText(getApplicationContext(), "u.odczyt", Toast.LENGTH_LONG).show();
        try {
            FileInputStream fis = openFileInput("user");
            DataInputStream dis = new DataInputStream(new BufferedInputStream(fis));

            _emailText.setText(dis.readUTF());
            _passwordText.setText(dis.readUTF());

            dis.close();
            //Toast.makeText(getApplicationContext(), "u.odczytano", Toast.LENGTH_LONG).show();
            isRegistered=true;
            if(isInternetConnection())
                login();
            else
            {
                offlineDialog();
            }
        } catch (Exception e)
        {
            isRegistered=false;
        }
    }

    private void offlineDialog()
    {
        AlertDialog.Builder adb = new AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_login_offline, null);
        Button loginOffline = (Button) dialogView.findViewById(R.id.btn_login_offline);
        TextView backLink = (TextView) dialogView.findViewById(R.id.link_back);
        adb.setView(dialogView);
        final AlertDialog dialog = adb.create();

        loginOffline.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v)
            {
                Toast.makeText(getApplicationContext(), "Tutaj zostanie dodane aktywności offline", Toast.LENGTH_LONG).show();
                //loginOffline();
            }
        });

        backLink.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                dialog.cancel();
                internetConnectionDialog();
            }
        });
        dialog.show();
    }

    public void loginOffline()
    {
        //TODO całe loginOffline
        Log.d(TAG, getString(R.string.login));
        _loginButton.setEnabled(false);

        final ProgressDialog progressDialog = new ProgressDialog(LoginActivity.this, R.style.AppTheme_Dark_Dialog);
        progressDialog.setIndeterminate(true);
        progressDialog.setMessage(getString(R.string.login));
        progressDialog.show();

        String email = _emailText.getText().toString().trim();
        String password = _passwordText.getText().toString().trim();

        firebaseAuth.signInWithEmailAndPassword(email, password).addOnCompleteListener(this, new OnCompleteListener<AuthResult>()
        {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task)
            {
                progressDialog.dismiss();
                if(task.isSuccessful())
                {
                    if(!isRegistered)
                        saveUser();
                    Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                    startActivityForResult(intent, REQUEST_SIGNUP);
                    finish();
                    overridePendingTransition(R.anim.push_left_in, R.anim.push_left_out);
                }
                else
                {
                    _emailText.setError(getString(R.string.login_failed));
                    _passwordText.setError(getString(R.string.login_failed));
                    _loginButton.setEnabled(true);
                }
            }
        });
    }
}