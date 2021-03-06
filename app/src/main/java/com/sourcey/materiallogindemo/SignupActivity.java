package com.sourcey.materiallogindemo;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import butterknife.BindView;
import butterknife.ButterKnife;

public class SignupActivity extends AppCompatActivity {
    private static final String TAG = "SignupActivity";
    private FirebaseAuth firebaseAuth;
    private DatabaseReference base;

    @BindView(R.id.input_name_first) EditText _nameFirstText;
    @BindView(R.id.input_name_middle) EditText _nameMiddleText;
    @BindView(R.id.input_name_last) EditText _nameLastText;
    @BindView(R.id.input_index_number) EditText _indexNumberText;
    @BindView(R.id.input_address) EditText _addressText;
    @BindView(R.id.input_pesel) EditText _peselText;
    @BindView(R.id.input_nrLegitymacji) EditText _nrLegitymacjiText;
    @BindView(R.id.input_password) EditText _passwordText;
    @BindView(R.id.input_reEnterPassword) EditText _reEnterPasswordText;
    @BindView(R.id.btn_signup) Button _signupButton;
    @BindView(R.id.link_login) TextView _loginLink;
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);
        ButterKnife.bind(this);

        firebaseAuth = FirebaseAuth.getInstance();
        base = FirebaseDatabase.getInstance().getReference().child("Studenci");

        _signupButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                signup();
            }
        });

        _loginLink.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                // Finish the registration screen and return to the Login activity
                Intent intent = new Intent(getApplicationContext(),LoginActivity.class);
                startActivity(intent);
                finish();
                overridePendingTransition(R.anim.push_right_in, R.anim.push_right_out);
            }
        });
    }

    public void signup()
    {
        Log.d(TAG, "Signup");

        if (!validate())
        {
            onSignupFailed();
            return;
        }

        _signupButton.setEnabled(false);

        final ProgressDialog progressDialog = new ProgressDialog(SignupActivity.this, R.style.AppTheme_Dark_Dialog);
        progressDialog.setIndeterminate(true);
        progressDialog.setMessage(getString(R.string.processing));
        progressDialog.show();

        String nameFirst = _nameFirstText.getText().toString().trim();
        String nameMiddle = _nameMiddleText.getText().toString().trim();
        String nameLast = _nameLastText.getText().toString().trim();
        String indexNumber = _indexNumberText.getText().toString().trim();
        String address = _addressText.getText().toString().trim();
        String email = indexNumber+"@student.pwr.edu.pl";
        String password = _passwordText.getText().toString().trim();
        String pesel = _peselText.getText().toString().trim();
        String nrLegitymacji = _nrLegitymacjiText.getText().toString().trim();
        String reEnterPassword = _reEnterPasswordText.getText().toString().trim();

        // Signup logic
        firebaseAuth.createUserWithEmailAndPassword(email, password);
        base = base.child(indexNumber);
        base.child("adres").setValue(address);
        base.child("drugieImie").setValue(nameMiddle);
        base.child("email").setValue(email);
        base.child("imie").setValue(nameFirst);
        base.child("nazwisko").setValue(nameLast);
        base.child("pesel").setValue(pesel);
        base.child("nrLegit").setValue(nrLegitymacji);




        new android.os.Handler().postDelayed(
                new Runnable() {
                    public void run() {
                        // On complete call either onSignupSuccess or onSignupFailed
                        // depending on success
                        onSignupSuccess();
                        // onSignupFailed();
                        progressDialog.dismiss();
                    }
                }, 3000);
    }




    public void onSignupSuccess()
    {
        Intent intent = new Intent(getApplicationContext(),LoginActivity.class);
        startActivity(intent);
        finish();
        overridePendingTransition(R.anim.push_left_in, R.anim.push_left_out);

        //_signupButton.setEnabled(true);
        //setResult(RESULT_OK, null);
        //finish();
    }

    public void onSignupFailed() {
        Toast.makeText(getBaseContext(), "Login failed", Toast.LENGTH_LONG).show();

        _signupButton.setEnabled(true);
    }

    public boolean validate() {
        boolean valid = true;
        //*
        String nameFirst = _nameFirstText.getText().toString().trim();
        String nameLast = _nameLastText.getText().toString().trim();
        String indexNumber = _indexNumberText.getText().toString().trim();
        String address = _addressText.getText().toString().trim();
        String pesel = _peselText.getText().toString().trim();
        String nrLegitymacji = _nrLegitymacjiText.getText().toString().trim();
        String password = _passwordText.getText().toString().trim();
        String reEnterPassword = _reEnterPasswordText.getText().toString().trim();

        if (nameFirst.isEmpty() || nameFirst.length() < 3)
        {
            _nameFirstText.setError(getString(R.string.name_first_error));
            valid = false;
        } else
        {
            _nameFirstText.setError(null);
        }

        if (nameLast.isEmpty() || nameLast.length() < 3)
        {
            _nameLastText.setError(getString(R.string.name_last_error));
            valid = false;
        } else
        {
            _nameLastText.setError(null);
        }

        if (indexNumber.isEmpty() || indexNumber.length() != 6)
        {
            _indexNumberText.setError(getString(R.string.index_number_error));
            valid = false;
        } else
        {
            _indexNumberText.setError(null);

        }

        if (pesel.isEmpty() || pesel.length() != 11)
        {
            _peselText.setError(getString(R.string.pesel_error));
            valid = false;
        } else
        {
            _peselText.setError(null);

        }

        if (nrLegitymacji.isEmpty() || nrLegitymacji.length() != 10)
        {
            _nrLegitymacjiText.setError(getString(R.string.nrLegitymacji_error));
            valid = false;
        } else
        {
            _nrLegitymacjiText.setError(null);

        }

        if (address.isEmpty())
        {
            _addressText.setError(getString(R.string.address_error));
            valid = false;
        } else
        {
            _addressText.setError(null);
        }



        if (password.isEmpty() || password.length() < 6)
        {
            _passwordText.setError(getString(R.string.password_error));
            valid = false;
        } else
        {
            _passwordText.setError(null);
        }

        if (reEnterPassword.isEmpty() || reEnterPassword.length() < 6 || !(reEnterPassword.equals(password)))
        {
            _reEnterPasswordText.setError(getString(R.string.password_re_enter_error));
            valid = false;
        } else {
            _reEnterPasswordText.setError(null);
        }
        //*/
        return valid;
    }


}