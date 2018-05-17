package com.sourcey.materiallogindemo;

import android.app.PendingIntent;
import android.content.Intent;
import android.content.IntentFilter;
import android.nfc.FormatException;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.Ndef;
import android.os.Parcelable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Iterator;
import java.util.Random;

import butterknife.BindView;

public class GenerateKeyActivity extends AppCompatActivity {

    TextView textProwadzacy;
    TextView textWybor;
    TextView textKod;
    TextView textNfcRead;
    Button butWyloguj;
    Button butGenerujKod;
    Button butZapisz;
    Button butOdswiez;
    Spinner spinnerKursy;
    ArrayAdapter<String> spinnerAdapter;
    ArrayList<String[]> daneKursow; //[0]nazwKursu, [1]idGrupy, [2]kodKursu, [3]dzienTyg, [4]godzRozp

    String idProwadzacy;
    String daneProwadzacy;
    String idGrupa;
    String[] elementySpinner;

    FirebaseAuth firebaseAuth;
    DatabaseReference database;

    //NFC
    public final String ERROR_DETECTED = "Nie wykryto taga NFC"; //zamienic na R.strings
    public final String WRITE_SUCCESS = "Pomyślnie zapisano na tagu";
    public final String WRITE_ERROR = "Zbliż tag jeszcze raz";
    NfcAdapter nfcAdapter;
    PendingIntent pendingIntent;
    IntentFilter writeTagFilters[];
    boolean writeMode;
    Tag myTag;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_generate_key);

        textKod = findViewById(R.id.text_kod);
        textProwadzacy = findViewById(R.id.text_prowadzacy);
        textWybor = findViewById(R.id.text_wybor);
        textNfcRead = findViewById(R.id.text_nfcread);
        spinnerKursy = findViewById(R.id.spinner_kursy);
        butGenerujKod = findViewById(R.id.but_generuj);
        butOdswiez = findViewById(R.id.but_odswiez);
        butWyloguj = findViewById(R.id.button_wyloguj);
        butZapisz = findViewById(R.id.but_zapisz);

        firebaseAuth = FirebaseAuth.getInstance();
        database = FirebaseDatabase.getInstance().getReference();
        daneKursow = new ArrayList<>();


        ustalIdProwadzacy();




        nfcAdapter = NfcAdapter.getDefaultAdapter(this);
        if (nfcAdapter == null) {
            Toast.makeText(this, "Twój telefon nie obsługuje NFC", Toast.LENGTH_LONG).show();
            finish();
        }

        readFromIntent(getIntent());

        pendingIntent = PendingIntent.getActivity(this, 0, new Intent(this, getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);
        IntentFilter tagDetected = new IntentFilter(NfcAdapter.ACTION_TAG_DISCOVERED);
        tagDetected.addCategory(Intent.CATEGORY_DEFAULT);
        writeTagFilters = new IntentFilter[] { tagDetected };

        butOdswiez.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                textProwadzacy.setText(daneProwadzacy);
                odswiezSpinner();
            }
        });

        butGenerujKod.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                generujKod();
            }
        });

        butWyloguj.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getApplicationContext(), LoginActivity.class);
                startActivity(intent);
                finish();
                overridePendingTransition(R.anim.push_right_in, R.anim.push_right_out);
            }
        });

        butZapisz.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    if (myTag == null) {
                        Toast.makeText(GenerateKeyActivity.this, ERROR_DETECTED, Toast.LENGTH_LONG).show();
                    } else {
                        write(textKod.getText().toString(), myTag);
                        Toast.makeText(GenerateKeyActivity.this, WRITE_SUCCESS, Toast.LENGTH_LONG ).show();
                    }
                } catch (IOException e) {
                    Toast.makeText(GenerateKeyActivity.this, WRITE_ERROR, Toast.LENGTH_LONG ).show();
                    e.printStackTrace();
                } catch (FormatException e) {
                    Toast.makeText(GenerateKeyActivity.this, WRITE_ERROR, Toast.LENGTH_LONG ).show();
                    e.printStackTrace();
                }
            }
        });


    }

    private void ustalIdProwadzacy(){
        final String emailProwadzacy = firebaseAuth.getCurrentUser().getEmail();
        database.child("Prowadzacy")
        .addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Iterator<DataSnapshot> iterator = dataSnapshot.getChildren().iterator();
                while (iterator.hasNext()){
                    DataSnapshot item = iterator.next();
                    String emailCurrent = item.child("email").getValue().toString();
                    if(emailCurrent.equals(emailProwadzacy)){
                        idProwadzacy = item.getKey().toString();
                        daneProwadzacy = item.child("tytul").getValue().toString()
                                +" "+ item.child("imie").getValue().toString()
                                +" "+item.child("nazwisko").getValue().toString();
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Toast.makeText(GenerateKeyActivity.this, "Error", Toast.LENGTH_LONG).show();
            }
        });

    }

    private void wypelnijDane(){
        database.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Iterator<DataSnapshot> iterator = dataSnapshot.child("Grupy").getChildren().iterator();
                while(iterator.hasNext()){
                    DataSnapshot grupa = iterator.next();
                    if(grupa.child("idProw").getValue().toString().equals(idProwadzacy)){
                        String nazwaKursu = dataSnapshot.child("Kursy").child(grupa.child("kodKursu").getValue().toString()).child("nazwa").getValue().toString().trim();
                        String[] kurs = new String[5];
                        kurs[0] = nazwaKursu;
                        kurs[1] = grupa.getKey().toString().trim();
                        kurs[2] = grupa.child("kodKursu").getValue().toString().trim();
                        kurs[3] = grupa.child("dzienTyg").getValue().toString().trim();
                        kurs[4] = grupa.child("godzRoz").getValue().toString().trim();
                        daneKursow.add(kurs);
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Toast.makeText(GenerateKeyActivity.this, "Error", Toast.LENGTH_LONG).show();
            }
        });
    }

    private void odswiezSpinner(){
        wypelnijDane();
        elementySpinner = new String[daneKursow.size()];
        for(int i=0; i<daneKursow.size(); i++){
            String[] arr = daneKursow.get(i);
            elementySpinner[i] = arr[0] + ", " + arr[3] + " " + arr[4];
        }
        spinnerAdapter = new ArrayAdapter<String>(GenerateKeyActivity.this, android.R.layout.simple_spinner_item, elementySpinner);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerKursy.setAdapter(spinnerAdapter);

        spinnerKursy.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                idGrupa = daneKursow.get(i)[1];
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });
    }

    private void generujKod() {
        StringBuilder sb = new StringBuilder();
        Random random = new Random();
        for(int i=0; i<6; i++){
            sb.append(random.nextInt(10));
        }
        String kod = sb.toString();
        textKod.setText(kod);

        if(idGrupa!=null) {
            database.child("Kody").child(idGrupa.toString()).child("kod").setValue(kod);
            database.child("Obecnosci").child(idGrupa.toString()).removeValue();
        }
    }


    //Zapisywanie na NFC
    private void write(String text, Tag tag) throws IOException, FormatException {
        NdefRecord[] records = { createRecord(text) };
        NdefMessage message = new NdefMessage(records);
        Ndef ndef = Ndef.get(tag);
        ndef.connect();
        ndef.writeNdefMessage(message);
        ndef.close();
    }
    private NdefRecord createRecord(String text) throws UnsupportedEncodingException {
        String lang       = "en";
        byte[] textBytes  = text.getBytes();
        byte[] langBytes  = lang.getBytes("US-ASCII");
        int    langLength = langBytes.length;
        int    textLength = textBytes.length;
        byte[] payload    = new byte[1 + langLength + textLength];

        payload[0] = (byte) langLength;

        System.arraycopy(langBytes, 0, payload, 1,              langLength);
        System.arraycopy(textBytes, 0, payload, 1 + langLength, textLength);

        NdefRecord recordNFC = new NdefRecord(NdefRecord.TNF_WELL_KNOWN,  NdefRecord.RTD_TEXT,  new byte[0], payload);

        return recordNFC;
    }

    private void readFromIntent(Intent intent) {
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

    private void buildTagViews(NdefMessage[] msgs) {
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

        textNfcRead.setText("Obecnie na tagu: " + text);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        setIntent(intent);
        readFromIntent(intent);
        if (NfcAdapter.ACTION_TAG_DISCOVERED.equals(intent.getAction())){
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
}
