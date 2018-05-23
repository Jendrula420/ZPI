package com.sourcey.materiallogindemo;

import android.app.AlertDialog;
import android.app.KeyguardManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.hardware.fingerprint.FingerprintManager;
import android.nfc.NdefMessage;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.os.Build;
import android.os.Parcelable;
import android.preference.PreferenceManager;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyPermanentlyInvalidatedException;
import android.security.keystore.KeyProperties;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Hashtable;
import java.util.Iterator;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;

@SuppressWarnings("ConstantConditions")
public class PresenceActivity extends AppCompatActivity {

    TextView textStudent;
    Button butWyloguj;
    Button butObecnosc;

    Button butStolowka;
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
    Tag myTag;
    String mode = "";

    //FINGERPRINT
    private KeyStore mKeyStore;
    private KeyGenerator mKeyGenerator;
    private SharedPreferences mSharedPreferences;
    Cipher defaultCipher;
    Cipher cipherNotInvalidated;

    //TODO Zmienic
    private static final String DIALOG_FRAGMENT_TAG = "myFragment";
    private static final String SECRET_MESSAGE = "Very secret message";
    private static final String KEY_NAME_NOT_INVALIDATED = "key_not_invalidated";
    static final String DEFAULT_KEY_NAME = "default_key";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_presence);

        textStudent = findViewById(R.id.text_student);
        butWyloguj = findViewById(R.id.but_wyloguj2);
        butObecnosc = findViewById(R.id.but_obecnosc);
        butStolowka = findViewById(R.id.but_stolowka);
        firebaseAuth = FirebaseAuth.getInstance();
        database = FirebaseDatabase.getInstance().getReference();
        nfcAdapter = NfcAdapter.getDefaultAdapter(this);
        if (nfcAdapter == null)
        {
            infoDialog("Twój telefon nie obsługuje NFC. Niestety będziesz musiał wprowadzać specjalne kody.", true);
            //TODO new activity manual input or inputMode
            noNFCMode = true;
        }

        readFromIntent(getIntent());

        pendingIntent = PendingIntent.getActivity(this, 0, new Intent(this, getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);
        IntentFilter tagDetected = new IntentFilter(NfcAdapter.ACTION_TAG_DISCOVERED);
        tagDetected.addCategory(Intent.CATEGORY_DEFAULT);
        writeTagFilters = new IntentFilter[] { tagDetected };

        butWyloguj.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getApplicationContext(), LoginActivity.class);
                startActivity(intent);
                finish();
                overridePendingTransition(R.anim.push_right_in, R.anim.push_right_out);
            }
        });

        butObecnosc.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view)
            {
                mode = "presence";
                infoDialog("Proszę zbliżyć tag NFC w celu odnotowania obecności na zajęciach.", false);
            }
        });

        butStolowka.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view)
            {
                mode = "cafeteria";
                infoDialog("Proszę zbliżyć tag NFC na stołówce studenckiej w celu wygenerowania kuponu.", false);
            }
        });


        //FINGERPRINT
        try {
            mKeyStore = KeyStore.getInstance("AndroidKeyStore");
        } catch (KeyStoreException e) {
            throw new RuntimeException("Failed to get an instance of KeyStore", e);
        }
        try {
            mKeyGenerator = KeyGenerator
                    .getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore");
        } catch (NoSuchAlgorithmException | NoSuchProviderException e) {
            throw new RuntimeException("Failed to get an instance of KeyGenerator", e);
        }
        try {
            defaultCipher = Cipher.getInstance(KeyProperties.KEY_ALGORITHM_AES + "/"
                    + KeyProperties.BLOCK_MODE_CBC + "/"
                    + KeyProperties.ENCRYPTION_PADDING_PKCS7);
            cipherNotInvalidated = Cipher.getInstance(KeyProperties.KEY_ALGORITHM_AES + "/"
                    + KeyProperties.BLOCK_MODE_CBC + "/"
                    + KeyProperties.ENCRYPTION_PADDING_PKCS7);
        } catch (NoSuchAlgorithmException | NoSuchPaddingException e) {
            throw new RuntimeException("Failed to get an instance of Cipher", e);
        }
        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        KeyguardManager keyguardManager = getSystemService(KeyguardManager.class);
        FingerprintManager fingerprintManager = getSystemService(FingerprintManager.class);



        if (!keyguardManager.isKeyguardSecure()) {
            Toast.makeText(this,
                    "Secure lock screen hasn't set up.\n"
                            + "Go to 'Settings -> Security -> Fingerprint' to set up a fingerprint",
                    Toast.LENGTH_LONG).show();
            return;
        }


        if (!fingerprintManager.hasEnrolledFingerprints()) {
            // This happens when no fingerprints are registered.
            Toast.makeText(this,
                    "Go to 'Settings -> Security -> Fingerprint' and register at least one" +
                            " fingerprint",
                    Toast.LENGTH_LONG).show();
            return;
        }
        createKey(DEFAULT_KEY_NAME, true);
        createKey(KEY_NAME_NOT_INVALIDATED, false);

    }

    @Override
    protected void onStart()
    {
        super.onStart();
        loadStudentData();
        if(!nfcAdapter.isEnabled())
        {
            //Intent intent = new Intent(Settings.ACTION_WIRELESS_SETTINGS);
            //startActivity(intent);
            //infoDialog("Twój telefon nie obsługuje NFC. Aby skorzystać z serwisu będziesz musiał wprowadzać ręcznie specjalne kody.", true);
            noNFCMode = true;
        }
    }

    private void getStudent()
    {
        final String emailStudent = firebaseAuth.getCurrentUser().getEmail();
        textStudent.setText(R.string.loading_data);
        database.child("Studenci").addListenerForSingleValueEvent(new ValueEventListener()
        {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot)
            {
                Iterator<DataSnapshot> iterator = dataSnapshot.getChildren().iterator();
                boolean nFound = true;
                while (iterator.hasNext() && nFound)
                {
                    DataSnapshot item = iterator.next();
                    if(emailStudent.equals(item.child("email").getValue().toString()))
                    {
                        values.put("studentName", item.child("imie").getValue().toString() +" "+
                                item.child("nazwisko").getValue().toString());
                        values.put("indexNumber", item.getKey());
                        String s = "Witaj "+values.get("studentName")
                                +"!\nNr albumu: "+values.get("indexNumber")+".";
                        saveStudentData();
                        textStudent.setText(s);
                        nFound = false;
                    }
                }
            }
            @Override
            public void onCancelled(DatabaseError databaseError) {}
        });
    }

    //odczyt NFC
    private void readFromIntent(Intent intent)
    {
        String m = mode;
        mode = "";
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
            buildTagViews(msgs, m);
        }
    }

    private void buildTagViews(NdefMessage[] msgs, String mode)
    {
        if (msgs == null || msgs.length == 0) return;

        String text = "";
        byte[] payload = msgs[0].getRecords()[0].getPayload();
        String textEncoding = ((payload[0] & 128) == 0) ? "UTF-8" : "UTF-16"; // Get the Text Encoding
        int languageCodeLength = payload[0] & 0063;

        try {
            text = new String(payload, languageCodeLength + 1, payload.length - languageCodeLength - 1, textEncoding);
        } catch (UnsupportedEncodingException e) {
            Log.e("UnsupportedEncoding", e.toString());
        }
        values.put("codeNFC", text);
        if(mode.equals("presence"))
            getGroupID();
        else if(mode.equals("cafeteria"))
        {
            //TODO verification cafeterias coupons
            getCoupon();
        }
    }

    private void getGroupID()
    {
        database.child("Kody").addListenerForSingleValueEvent(new ValueEventListener()
        {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot)
            {
                Iterator<DataSnapshot> iterator = dataSnapshot.getChildren().iterator();
                boolean nFound = true;
                String code = values.get("codeNFC");
                while (iterator.hasNext() && nFound)
                {
                    DataSnapshot item = iterator.next();
                    if(code.equals(item.child("kod").getValue().toString()))
                    {
                        values.put("groupID", item.getKey());
                       // values.put("classesNumber", item.child("zajecia").getValue().toString());
                        nFound = false;
                        getGroup();
                    }
                }
            }
            @Override
            public void onCancelled(DatabaseError databaseError) {}
        });
    }

    private void getGroup()
    {
        database.child("Grupy").addListenerForSingleValueEvent(new ValueEventListener()
        {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot)
            {
                Iterator<DataSnapshot> iterator = dataSnapshot.getChildren().iterator();
                boolean nFound = true;
                String group = values.get("groupID");
                while (iterator.hasNext() && nFound)
                {
                    DataSnapshot item = iterator.next();
                    if(group.equals( item.getKey() ))
                    {
                        nFound = false;
                        values.put("lecturerID", item.child("idProw").getValue().toString());
                        values.put("courseID", item.child("kodKursu").getValue().toString());
                        String tp = item.child("tydzienParz").getValue().toString();
                        values.put("classesDate",  item.child("dzienTyg").getValue().toString()
                                +((tp.equals("true"))?"tydzień parzysty":(tp.equals("false"))?"tydzień nieparzysty":""));
                        values.put("classesPlace",   item.child("budynek").getValue().toString()
                                +" "+item.child("sala").getValue().toString());
                        values.put("classesTime",    item.child("godzRoz").getValue().toString()
                                +"-"+item.child("godzZak").getValue().toString());
                        getLecturer();
                        getCourse();
                    }
                }
            }
            @Override
            public void onCancelled(DatabaseError databaseError) {}
        });
    }

    private void getLecturer()
    {
        database.child("Prowadzacy").addListenerForSingleValueEvent(new ValueEventListener()
        {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot)
            {
                Iterator<DataSnapshot> iterator = dataSnapshot.getChildren().iterator();
                boolean nFound = true;
                String lecturer = values.get("lecturerID");
                while (iterator.hasNext() && nFound)
                {
                    DataSnapshot item = iterator.next();
                    if(lecturer.equals( item.getKey() ))
                    {
                        values.put("lecturer",   item.child("tytul").getValue().toString()
                                +" "+item.child("imie").getValue().toString()
                                +" "+item.child("nazwisko").getValue().toString());
                        nFound = false;
                    }
                }
            }
            @Override
            public void onCancelled(DatabaseError databaseError) {}
        });
    }

    private void getCourse()
    {
        database.child("Kursy").addListenerForSingleValueEvent(new ValueEventListener()
        {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot)
            {
                Iterator<DataSnapshot> iterator = dataSnapshot.getChildren().iterator();
                boolean nFound = true;
                String course = values.get("courseID");
                while (iterator.hasNext() && nFound)
                {
                    DataSnapshot item = iterator.next();
                    if(course.equals( item.getKey() ))
                    {
                        values.put("courseName", item.child("nazwa").getValue().toString() );
                        nFound = false;
                        values.get("lecturer");
                        presenceDialog();
                    }
                }
            }
            @Override
            public void onCancelled(DatabaseError databaseError) {}
        });
    }

    private void presenceDialog()
    {
        AlertDialog.Builder adb = new AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_note_presence, null);
        Button presence = dialogView.findViewById(R.id.buttonNPPresence);
        Button cancel   = dialogView.findViewById(R.id.buttonNPCancel);
        String message ="Kurs: "+values.get("courseName")
                +"\nProwadzący: "+values.get("lecturer")
                +"\nCzas zajęć: "+values.get("classesDate")+": "+values.get("classesTime")
                +"\nMiejsce zajęć: "+values.get("classesPlace")
                +"\nZajęcia numer: "+values.get("classesNumber");
        values.put("message", message);
        TextView tv = dialogView.findViewById(R.id.textNP);
        tv.setText(message);
        adb.setView(dialogView);
        final AlertDialog dialog = adb.create();

        presence.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v)
            {
                dialog.cancel();
                zapiszObecnosc();
            }
        });

        cancel.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                dialog.cancel();
            }
        });
        dialog.show();
    }

    //zapis obecności na podstawie NFC

    public void potwierdzObecnosc(){
            String p = values.get("classesNumber")+"."+
                    new SimpleDateFormat("yy-MM-dd.HH:mm").format(Calendar.getInstance().getTime());


            //TODO add to p geolocalization and imei
            database.child("Obecnosci").child(values.get("groupID")).child(values.get("indexNumber")).setValue(p)
                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            if(task.isSuccessful())
                                infoDialog("Twoja obecność została odnotowana na zjęciach:\n"+values.get("message"), true);
                            else
                                infoDialog("Wystąpił błąd!", true);
                        }
                    });
    }


    private void zapiszObecnosc()
    {
        PurchaseFingerPrint finger = new PurchaseFingerPrint(defaultCipher, DEFAULT_KEY_NAME);
        finger.check();

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

    private void getCoupon()
    {
        values.put("date", new SimpleDateFormat("yy-MM-dd").format(Calendar.getInstance().getTime()));
        database.child("Kupony").addListenerForSingleValueEvent(new ValueEventListener()
        {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot)
            {
                Iterator<DataSnapshot> iterator = dataSnapshot.getChildren().iterator();
                boolean nFound = true;
                String v = values.get("date");
                while (iterator.hasNext() && nFound)
                {
                    DataSnapshot item = iterator.next();
                    if(v.equals( item.getKey() ))
                    {
                        String id = values.get("indexNumber");
                        v = item.getValue().toString();
                        v = v.replaceAll("[{},]", "");
                        v = v.replaceAll("=[0-2][0-9]:[0-5][0-9]", "");
                        if(v.length()<7)
                        {
                            if (!v.equals(id))
                                generateCoupon();
                            else
                                infoDialog("Dzisiejszy kupon został już wykorzystany o godzinie: "
                                        +item.child(id).getValue().toString()+".", true);
                        }
                        else
                        {
                            if (!found(v.split(" "), id))
                                generateCoupon();
                            else
                                infoDialog("Dzisiejszy kupon został już wykorzystany o godzinie: "
                                        +item.child(id).getValue().toString()+".", true);
                        }
                        nFound = false;
                    }
                }
                if(nFound)
                    generateCoupon();
            }
            @Override
            public void onCancelled(DatabaseError databaseError) {}
        });
    }

    protected void generateCoupon()
    {
        values.put("time", new SimpleDateFormat("HH:mm").format(Calendar.getInstance().getTime()));
        database.child("Kupony").child(values.get("date")).child(values.get("indexNumber")).setValue(values.get("time"))
            .addOnCompleteListener(new OnCompleteListener<Void>() {
                @Override
                public void onComplete(@NonNull Task<Void> task) {
                    if(task.isSuccessful())
                        infoDialog("Kupon został przyznany.\nCzas przyznania: "
                                +(values.get("time"))+".\nProszę pokazać komunikat ekspedjentce przed zamknięciem.", true);
                    else
                        infoDialog("Wystąpił błąd!", true);
                }
            });
    }

    protected boolean found(String[] IDs, String searchedID)
    {
        boolean result = true;
        int i =0;
        while(i<IDs.length && result)
        {
            if(IDs[i].equals(searchedID))
                result = false;
        }
        return !result;
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
    }

    @Override
    public void onResume(){
        super.onResume();
        WriteModeOn();
    }

    private void WriteModeOn(){
        writeMode = true;
        nfcAdapter.enableForegroundDispatch(this, pendingIntent, writeTagFilters, null);
    }

    private void WriteModeOff(){
        writeMode = false;
        nfcAdapter.disableForegroundDispatch(this);
    }

    public void saveStudentData()
    {
        //TODO szyfrowanie danych
        try {
            FileOutputStream os = openFileOutput("studentData", Context.MODE_PRIVATE);
            DataOutputStream dos = new DataOutputStream(new BufferedOutputStream(os));
            dos.writeUTF(values.get("indexNumber"));
            dos.writeUTF(values.get("studentName"));
            dos.close();
        } catch (Exception e) { textStudent.setText(e.getMessage()); }
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
        } catch (IOException e) { getStudent(); }
    }

    //FINGERPRINT ZONE

    public void createKey(String keyName, boolean invalidatedByBiometricEnrollment) {
        try {
            mKeyStore.load(null);


            KeyGenParameterSpec.Builder builder = new KeyGenParameterSpec.Builder(keyName,
                    KeyProperties.PURPOSE_ENCRYPT |
                            KeyProperties.PURPOSE_DECRYPT)
                    .setBlockModes(KeyProperties.BLOCK_MODE_CBC)

                    .setUserAuthenticationRequired(true)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_PKCS7);


            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                builder.setInvalidatedByBiometricEnrollment(invalidatedByBiometricEnrollment);
            }
            mKeyGenerator.init(builder.build());
            mKeyGenerator.generateKey();
        } catch (NoSuchAlgorithmException | InvalidAlgorithmParameterException
                | CertificateException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void onPurchased(boolean withFingerprint,
                            @Nullable FingerprintManager.CryptoObject cryptoObject) {
        if (withFingerprint) {
            // If the user has authenticated with fingerprint, verify that using cryptography and
            // then show the confirmation message.
            assert cryptoObject != null;
            tryEncrypt(cryptoObject.getCipher());
        } else {
            // Authentication happened with backup password. Just show the confirmation message.
           // showConfirmation(null);
        }
        potwierdzObecnosc();
    }

    private boolean initCipher(Cipher cipher, String keyName) {
        try {
            mKeyStore.load(null);
            SecretKey key = (SecretKey) mKeyStore.getKey(keyName, null);
            cipher.init(Cipher.ENCRYPT_MODE, key);
            return true;
        } catch (KeyPermanentlyInvalidatedException e) {
            return false;
        } catch (KeyStoreException | CertificateException | UnrecoverableKeyException | IOException
                | NoSuchAlgorithmException | InvalidKeyException e) {
            throw new RuntimeException("Failed to init Cipher", e);
        }
    }

//    private void showConfirmation(byte[] encrypted) {
//        findViewById(R.id.confirmation_message).setVisibility(View.VISIBLE);
//        if (encrypted != null) {
//            TextView v = findViewById(R.id.encrypted_message);
//            v.setVisibility(View.VISIBLE);
//            v.setText(Base64.encodeToString(encrypted, 0 /* flags */));
//        }
//    }

    private void tryEncrypt(Cipher cipher) {
        try {
            byte[] encrypted = cipher.doFinal(SECRET_MESSAGE.getBytes());
           // showConfirmation(encrypted);
        } catch (BadPaddingException | IllegalBlockSizeException e) {
            Toast.makeText(this, "Failed to encrypt the data with the generated key. "
                    + "Retry the purchase", Toast.LENGTH_LONG).show();
            //Log.e(TAG, "Failed to encrypt the data with the generated key." + e.getMessage());
        }
    }

    private class PurchaseFingerPrint {

        Cipher mCipher;
        String mKeyName;

        PurchaseFingerPrint(Cipher cipher, String keyName) {
            mCipher = cipher;
            mKeyName = keyName;
        }

        public void check() {

            if (initCipher(mCipher, mKeyName)) {

                FingerprintAuthentication fragment
                        = new FingerprintAuthentication();
                fragment.setCryptoObject(new FingerprintManager.CryptoObject(mCipher));
                fragment.setPassword(firebaseAuth.getCurrentUser().getEmail().toString().trim());
                boolean useFingerprintPreference = mSharedPreferences
                        .getBoolean(getString(R.string.use_fingerprint_to_authenticate_key),
                                true);
                if (useFingerprintPreference) {
                    fragment.setStage(
                            FingerprintAuthentication.Stage.FINGERPRINT);
                } else {
                    fragment.setStage(
                            FingerprintAuthentication.Stage.PASSWORD);
                }
                fragment.show(getFragmentManager(), DIALOG_FRAGMENT_TAG);
            } else {
                FingerprintAuthentication fragment
                        = new FingerprintAuthentication();
                fragment.setCryptoObject(new FingerprintManager.CryptoObject(mCipher));
                fragment.setStage(
                        FingerprintAuthentication.Stage.NEW_FINGERPRINT_ENROLLED);
                fragment.show(getFragmentManager(), DIALOG_FRAGMENT_TAG);
            }
        }
    }
}