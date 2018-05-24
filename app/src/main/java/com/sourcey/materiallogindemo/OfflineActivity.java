package com.sourcey.materiallogindemo;

import android.app.AlertDialog;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.nfc.NdefMessage;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.os.Bundle;
import android.os.Parcelable;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.telephony.TelephonyManager;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Hashtable;

@SuppressWarnings("ConstantConditions")
public class OfflineActivity extends AppCompatActivity
{
    TextView textStudent;
    Button butWyloguj;
    Button butObecnosc;
    EditText textInputDialog;

    InputMethodManager imm;
    Button butStolowka;
    Button butOffline;
    AlertDialog dialog;
    Hashtable<String, String> values = new Hashtable<>();

    FirebaseAuth firebaseAuth;
    DatabaseReference database;

    //NFC
    NfcAdapter nfcAdapter;
    PendingIntent pendingIntent;
    IntentFilter writeTagFilters[];
    boolean writeMode;
    boolean noNFCMode;
    boolean noInternet;
    Tag myTag;
    String mode = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_presence);

        textStudent = findViewById(R.id.text_student);
        butWyloguj = findViewById(R.id.but_wyloguj2);
        butOffline = findViewById(R.id.button_offline);
        butOffline.setVisibility(View.VISIBLE);
        butWyloguj.setVisibility(View.INVISIBLE);
        butObecnosc = findViewById(R.id.but_obecnosc);
        butStolowka = findViewById(R.id.but_stolowka);
        butStolowka.setEnabled(false);
        firebaseAuth = FirebaseAuth.getInstance();
        database = FirebaseDatabase.getInstance().getReference();
        nfcAdapter = NfcAdapter.getDefaultAdapter(this);
        if (nfcAdapter == null)
        {
            TextView tv = findViewById(R.id.text_noNFCInfo);
            tv.setText(getString(R.string.no_nfc_message_p1));
            tv.append("\n"+getString(R.string.no_nfc_message_p2));
            noNFCMode = true;
        }

        readFromIntent(getIntent());

        pendingIntent = PendingIntent.getActivity(this, 0, new Intent(this, getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);
        IntentFilter tagDetected = new IntentFilter(NfcAdapter.ACTION_TAG_DISCOVERED);
        tagDetected.addCategory(Intent.CATEGORY_DEFAULT);
        writeTagFilters = new IntentFilter[] { tagDetected };

        butOffline.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view)
            {
                tryLogin();
            }
        });

        butObecnosc.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view)
            {
                offlinePresenceDialog(getString(R.string.no_internet_warning));
            }
        });

        butStolowka.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view)
            {
                infoDialog(getString(R.string.cafeteria_no_internet_message), true);
            }
        });
    }
    //TODO load and send on server code with scan time
    @Override
    protected void onStart()
    {
        super.onStart();
        loadUser();
        loadStudentData();
        tryLogin();
        TextView tv = findViewById(R.id.text_noNFCInfo);
        tv.setText(R.string.offline_message);
        if(!nfcAdapter.isEnabled())
        {
            //TODO uncomment before presentation
            /*
            Intent intent = new Intent(Settings.ACTION_WIRELESS_SETTINGS);
            startActivity(intent);
            //*/
            //TODO remove before presentation
            tv.append("\n"+getString(R.string.no_nfc_message_p1));
            tv.append("\n"+getString(R.string.no_nfc_message_p2));
            noNFCMode = true;
        }
        //justify(tv);
    }

    public void tryLogin()
    {
        checkInternet();
        if(!noInternet)
        {
            final ProgressDialog progressDialog = new ProgressDialog(OfflineActivity.this, R.style.AppTheme_Dark_Dialog);
            progressDialog.setIndeterminate(true);
            progressDialog.setMessage(getString(R.string.login));
            progressDialog.show();

            String email = values.get("email");
            String password = values.get("password");

            firebaseAuth.signInWithEmailAndPassword(email, password).addOnCompleteListener(
                    this, new OnCompleteListener<AuthResult>() {
                @Override
                public void onComplete(@NonNull Task<AuthResult> task)
                {
                    progressDialog.dismiss();
                    if(task.isSuccessful())
                    {
                        Intent intent = new Intent(getApplicationContext(), PresenceActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
                        startActivity(intent);
                        overridePendingTransition(0,0);
                        finish();
                    }
                    else
                        Toast.makeText(getBaseContext(), getString(R.string.login_failed), Toast.LENGTH_LONG).show();
                }
            });
        }
    }

    //odczyt NFC
    private void readFromIntent(Intent intent)
    {
        String action = intent.getAction();
        if (NfcAdapter.ACTION_TAG_DISCOVERED.equals(action)
                || NfcAdapter.ACTION_TECH_DISCOVERED.equals(action)
                || NfcAdapter.ACTION_NDEF_DISCOVERED.equals(action)) {
            Parcelable[] rawMsgs = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);
            NdefMessage[] msgs = null;
            if (rawMsgs != null) {
                msgs = new NdefMessage[rawMsgs.length];
                for (int i = 0; i < rawMsgs.length; i++) {
                    msgs[i] = (NdefMessage) rawMsgs[i];
                }
            }
            buildTagViews(msgs);
        }
    }

    private void buildTagViews(NdefMessage[] msgs)
    {
        if (msgs == null || msgs.length == 0)
        {
            String text = "";
            byte[] payload = msgs[0].getRecords()[0].getPayload();
            String textEncoding = ((payload[0] & 128) == 0) ? "UTF-8" : "UTF-16"; // Get the Text Encoding
            int languageCodeLength = payload[0] & 0x33;
            try {
                text = new String(payload, languageCodeLength + 1, payload.length - languageCodeLength - 1, textEncoding);
            } catch (UnsupportedEncodingException e) {
                Log.e("UnsupportedEncoding", e.toString());
            }
            saveData(text, "codeNFC");
        }
    }

    private void offlinePresenceDialog(String message)
    {
        AlertDialog.Builder adb = new AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_note_presence, null);
        Button presence = dialogView.findViewById(R.id.buttonNPPresence);
        presence.setText(R.string.try_connect_again);
        Button cancel   = dialogView.findViewById(R.id.buttonNPCancel);
        cancel.setText(R.string.save_code);
        TextView tv = dialogView.findViewById(R.id.textNP);
        tv.setText(message);
        adb.setView(dialogView);
        final AlertDialog dialog = adb.create();

        presence.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v)
            {
                tryLogin();
            }
        });

        cancel.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                dialog.cancel();
                mode = "presence";
                if (noNFCMode)
                    inputDialog(getString(R.string.presence_no_nfc_message));
                else
                    infoDialog(getString(R.string.presence_nfc_message), false);
            }
        });
        dialog.show();
    }

    private void infoDialog(String message, boolean ok)
    {
        AlertDialog.Builder adb = new AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_info, null);
        Button close = dialogView.findViewById(R.id.buttonInfoClose);
        if(!ok)
            close.setText(R.string.cancel);
        TextView tv = dialogView.findViewById(R.id.textInfo);
        tv.setText(message);
        adb.setView(dialogView);
        final AlertDialog dialog = adb.create();
        this.dialog = dialog;

        close.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v)
            {
                dialog.cancel();
                mode = "";
            }
        });
        dialog.show();
    }

    private void inputDialog(String message)
    {
        AlertDialog.Builder adb = new AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_input, null);
        Button cancel = dialogView.findViewById(R.id.buttonCancelInputDialog);
        Button submit = dialogView.findViewById(R.id.buttonSubmitInputDialog);
        TextView tv = dialogView.findViewById(R.id.textDialog);
        tv.setText(message);
        adb.setView(dialogView);
        final AlertDialog dialog = adb.create();

        submit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v)
            {
                String mi = textInputDialog.getText().toString();
                if(verifyCode(mi))
                {
                    saveData(mi, "codeManual");
                }
                else
                    infoDialog(getString(R.string.wrong_code), true);
                dialog.cancel();
                mode = "";
            }
        });
        cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v)
            {
                dialog.cancel();
                mode = "";
            }
        });
        dialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                imm.toggleSoftInput(InputMethodManager.HIDE_IMPLICIT_ONLY, 0);
                imm = null;
            }
        });

        dialog.show();
        textInputDialog = dialogView.findViewById(R.id.textInputDialog);
        textInputDialog.setInputType(InputType.TYPE_CLASS_NUMBER);
        //TODO remove before presentation
        textInputDialog.setText(("352845"));
        textInputDialog.requestFocus();
        imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);
    }

    private boolean verifyCode(String code)
    {
        return code.length()==6 && code.matches("^[0-9]*$");
    }

    @Override
    protected void onNewIntent(Intent intent)
    {
        if(!mode.equals(""))
        {
            dialog.cancel();
            setIntent(intent);
            readFromIntent(intent);
            if (NfcAdapter.ACTION_TAG_DISCOVERED.equals(intent.getAction()))
                myTag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
        }
    }

    @Override
    public void onPause(){
        super.onPause();
        WriteModeOff();
        if(imm!=null)
            imm.toggleSoftInput(InputMethodManager.HIDE_IMPLICIT_ONLY, 0);
    }

    @Override
    public void onResume()
    {
        super.onResume();
        WriteModeOn();
        checkInternet();
    }

    private void checkInternet()
    {
        if(!isInternetConnection())
        {
            noInternet = true;
            butOffline.setVisibility(View.VISIBLE);
        }
        else
        {
            noInternet = false;
            butOffline.setVisibility(View.INVISIBLE);
        }
    }

    private void WriteModeOn(){
        writeMode = true;
        nfcAdapter.enableForegroundDispatch(this, pendingIntent, writeTagFilters, null);
    }

    private void WriteModeOff(){
        writeMode = false;
        nfcAdapter.disableForegroundDispatch(this);
    }

    public void saveData(String code, String mode)
    {
        //TODO data encryption
        //TODO add to p geolocalization
        try {
            String[] codes = loadCodes(mode);
            FileOutputStream os = openFileOutput(mode, Context.MODE_PRIVATE);
            DataOutputStream dos = new DataOutputStream(new BufferedOutputStream(os));
            boolean nSaved = true;
            //textStudent.setText("");///////////////////////////////////////////////////////////////
            for(int i = 0; i<codes.length; i++)
            {
                //textStudent.append("\n"+codes[i]);
                String[] a = codes[i].split("[;]");
                if(code.equals(a[0]))
                    nSaved=false;
                dos.writeUTF(codes[i]);
            }
            if(nSaved)
            {
                dos.writeUTF(code+";"+
                        values.get("indexNumber")+";"+
                        new SimpleDateFormat("yy-MM-dd;HH:mm").format(Calendar.getInstance().getTime())+";"+
                        getDeviceID()+";"+
                        "W"+";"+
                        "E"+";");
                infoDialog("Kod został zapisany.", true);
            }
            else
                infoDialog(getString(R.string.saved_code_message), true);
            dos.close();
        }catch (Exception e) { textStudent.setText(e.getMessage()); }
    }

    public String[] loadCodes(String mode)
    {
        ArrayList<String> result = new ArrayList<>();
        try {
            FileInputStream fis = openFileInput(mode);
            DataInputStream dis = new DataInputStream(new BufferedInputStream(fis));
            while(dis.available()>0)
                result.add(dis.readUTF());
            dis.close();
        }catch (Exception e) { return new String[0]; }
        return result.size()>0?result.toArray(new String[result.size()]):new String[0];
    }

    public void loadStudentData()
    {
        try {
            FileInputStream fis = openFileInput("studentData");
            DataInputStream dis = new DataInputStream(new BufferedInputStream(fis));
            values.put("indexNumber", dis.readUTF());
            values.put("studentName", dis.readUTF());
            dis.close();
            String s = "Witaj "+values.get("studentName")
                    +"!\nNr albumu: "+values.get("indexNumber")+".";
            textStudent.setText(s);
        } catch (IOException e) { textStudent.setText(e.getMessage()); }
    }

    public void loadUser()
    {
        try {
            FileInputStream fis = openFileInput("user");
            DataInputStream dis = new DataInputStream(new BufferedInputStream(fis));
            values.put("email", dis.readUTF());
            values.put("password", dis.readUTF());
            dis.close();
        } catch (Exception e) { textStudent.setText(e.getMessage()); }
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

    public String getDeviceID()
    {
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
}