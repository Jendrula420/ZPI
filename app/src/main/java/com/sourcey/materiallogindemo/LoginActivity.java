package com.sourcey.materiallogindemo;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;

import butterknife.BindView;
import butterknife.ButterKnife;

public class LoginActivity extends AppCompatActivity {
    private static final String TAG = "LoginActivity";
    private static final int REQUEST_SIGNUP = 0;
    boolean student;
    private FirebaseAuth firebaseAuth;

    @BindView(R.id.input_email) EditText _emailText;
    @BindView(R.id.input_password) EditText _passwordText;
    @BindView(R.id.btn_login) Button _loginButton;
    @BindView(R.id.link_signup) TextView _signupLink;
    @BindView(R.id.switch_czyStudent) Switch _studentSwitch;
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        ButterKnife.bind(this);
        student = true;

        firebaseAuth = FirebaseAuth.getInstance();

        if(firebaseAuth.getCurrentUser() != null)
            firebaseAuth.signOut();

        _studentSwitch.setText("Logujesz się jako: Student");
        _studentSwitch.setChecked(true);
        
        _loginButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                login();
            }
        });

        _signupLink.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                // Start the Signup activity
                Intent intent = new Intent(getApplicationContext(), SignupActivity.class);
                startActivityForResult(intent, REQUEST_SIGNUP);
                finish();
                overridePendingTransition(R.anim.push_left_in, R.anim.push_left_out);
            }
        });

        _studentSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if(b){
                    compoundButton.setText("Logujesz się jako: Student");
                    student = true;
                }
                else{
                    compoundButton.setText("Logujesz się jako: Prowadzący");
                    student = false;
                }
            }
        });


    }

    public void login() {
        Log.d(TAG, getString(R.string.login));

        if (!validate()) {
            Toast.makeText(getBaseContext(), getString(R.string.login_failed), Toast.LENGTH_LONG).show();
            _loginButton.setEnabled(true);
            return;
        }

        _loginButton.setEnabled(false);

        final ProgressDialog progressDialog = new ProgressDialog(LoginActivity.this,
                R.style.AppTheme_Dark_Dialog);
        progressDialog.setIndeterminate(true);
        progressDialog.setMessage(getString(R.string.login));
        progressDialog.show();

        String email = _emailText.getText().toString().trim();
        String password = _passwordText.getText().toString().trim();

        firebaseAuth.signInWithEmailAndPassword(email, password).addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {
                progressDialog.dismiss();
                if(task.isSuccessful()){
                    Intent intent;
                    if(student)
                        intent = new Intent(getApplicationContext(), PresenceActivity.class);
                    else
                        //change
                        intent = new Intent(getApplicationContext(), GenerateKeyActivity.class);
                    startActivityForResult(intent, REQUEST_SIGNUP);
                    finish();
                    overridePendingTransition(R.anim.push_left_in, R.anim.push_left_out);
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

        if (email.isEmpty() || email.length() < 6)
        {
            _emailText.setError(getString(R.string.e_mail_error));
            valid = false;
        } else
        {
            if(email.length() == 6)
            {
                //if(emial.isNumeric())
                if(student)
                    email+="@student.pwr.edu.pl";
                else
                    email+="@pwr.edu.pl";
                _emailText.setText(email);
            }
            else if(!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches())
            {
                _emailText.setError(getString(R.string.e_mail_error));
                valid = false;
            }
            else
            {
                _emailText.setError(null);
            }
        }

        if (password.isEmpty() || password.length() < 6)
        {
            _passwordText.setError(getString(R.string.password_error));
            valid = false;
        } else
        {
            _passwordText.setError(null);
        }
        //*/
        return valid;
    }
}