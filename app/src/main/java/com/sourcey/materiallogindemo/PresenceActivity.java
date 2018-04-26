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
import android.support.annotation.NonNull;
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

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.w3c.dom.Text;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;

public class PresenceActivity extends AppCompatActivity {

    TextView textStudent;
    TextView textWybor;
    TextView textKurs;
    Button butOdswiez;
    Button butWyloguj;
    Button butUstal;
    Button butObecnosc;
    Spinner spinnerProwadzacy;
    ArrayAdapter<String> spinnerAdapter;


    ArrayList<String> elementySpinner;
    ArrayList<String> prowadzacyList;
    String nrAlbumu;
    String daneStudent;
    String wybranyProw;
    String wybranyKurs;
    String idGrupy;
    String kod;

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
        setContentView(R.layout.activity_presence);

        textStudent = findViewById(R.id.text_student);
        textWybor = findViewById(R.id.text_wybor2);
        textKurs = findViewById(R.id.text_kurs);
        butOdswiez = findViewById(R.id.but_odswiez2);
        butWyloguj = findViewById(R.id.but_wyloguj2);
        butUstal = findViewById(R.id.but_ustal);
        butObecnosc = findViewById(R.id.but_obecnosc);
        spinnerProwadzacy = findViewById(R.id.spinner_prowadzacy);

        elementySpinner = new ArrayList<>();
        prowadzacyList = new ArrayList<>();
        firebaseAuth = FirebaseAuth.getInstance();
        database = FirebaseDatabase.getInstance().getReference();

        ustalIdStudent();

        butObecnosc.setEnabled(false);


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
                textStudent.setText(daneStudent);
                odswiezSpinner();
                wybranyKurs = null;
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

        butUstal.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ustalKurs();
                if(wybranyKurs == null)
                    textKurs.setText("Nie ustalono kursu");
                else {
                    textKurs.setText(wybranyKurs);
                    butObecnosc.setEnabled(true);
                }

            }
        });

        butObecnosc.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                Toast.makeText(PresenceActivity.this, "Zbliż tag", Toast.LENGTH_LONG).show();



            }
        });

    }

    private void ustalIdStudent(){
        final String emailStudent = firebaseAuth.getCurrentUser().getEmail();
        database.child("Studenci")
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        Iterator<DataSnapshot> iterator = dataSnapshot.getChildren().iterator();
                        while (iterator.hasNext()){
                            DataSnapshot item = iterator.next();
                            String emailCurrent = item.child("email").getValue().toString();
                            if(emailCurrent.equals(emailStudent)){
                                nrAlbumu = item.getKey().toString();
                                daneStudent = item.child("imie").getValue().toString()
                                        +" "+item.child("nazwisko").getValue().toString();
                            }
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                        Toast.makeText(PresenceActivity.this, "Error", Toast.LENGTH_LONG).show();
                    }
                });

    }

    private void odswiezSpinner(){
        database.child("Prowadzacy").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Iterator<DataSnapshot> iterator = dataSnapshot.getChildren().iterator();
                while (iterator.hasNext()){
                    DataSnapshot prow = iterator.next();
                    String prowadzacy = prow.child("tytul").getValue().toString()
                            +" "+prow.child("imie").getValue().toString()
                            +" "+prow.child("nazwisko").getValue().toString();
                    elementySpinner.add(prowadzacy);
                    prowadzacyList.add(prow.getKey());
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Toast.makeText(PresenceActivity.this, "Error", Toast.LENGTH_LONG).show();
            }
        });

        spinnerAdapter = new ArrayAdapter<String>(PresenceActivity.this, android.R.layout.simple_spinner_item, elementySpinner);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerProwadzacy.setAdapter(spinnerAdapter);

        spinnerProwadzacy.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                wybranyProw = prowadzacyList.get(i);
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });
    }

    private void ustalKurs(){
        database.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Iterator<DataSnapshot> iterator = dataSnapshot.child("Grupy").getChildren().iterator();
                while(iterator.hasNext()){
                    DataSnapshot grupa = iterator.next();
                    if(grupa.child("idProw").getValue().toString().equals(wybranyProw)){
                        Calendar calendar = Calendar.getInstance();
                        int dayAkt = calendar.get(Calendar.DAY_OF_WEEK);
                        int dayRozp = Calendar.SATURDAY;

                        if(grupa.child("dzienTyg").getValue().toString().equals("Poniedziałek"))
                            dayRozp = Calendar.MONDAY;
                        else if(grupa.child("dzienTyg").getValue().toString().equals("Wtorek"))
                            dayRozp = Calendar.TUESDAY;
                        else if(grupa.child("dzienTyg").getValue().toString().equals("Środa"))
                            dayRozp = Calendar.WEDNESDAY;
                        else if(grupa.child("dzienTyg").getValue().toString().equals("Czwartek"))
                            dayRozp = Calendar.THURSDAY;
                        else if(grupa.child("dzienTyg").getValue().toString().equals("Piątek"))
                            dayRozp = Calendar.FRIDAY;


                        String godzRozp = grupa.child("godzRoz").getValue().toString().trim();
                        String godzZak = grupa.child("godzZak").getValue().toString().trim();
                        Calendar cal = Calendar.getInstance();
                        DateFormat formatter = new SimpleDateFormat("HH:mm");
                        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm");
                        Date gRoz = new Date();
                        Date gZak = new Date();
                        Date gAkt = new Date();
                        try {
                            gRoz = (Date)formatter.parse(godzRozp);
                            gZak = (Date)formatter.parse(godzZak);
                            gAkt = (Date)formatter.parse(sdf.format(cal.getTime()));
                        } catch (ParseException e) {
                            e.printStackTrace();
                        }
                        if(gAkt.after(gRoz) && gAkt.before(gZak) && dayAkt == dayRozp){
                            idGrupy = grupa.getKey();
                            wybranyKurs = dataSnapshot.child("Kursy").child(grupa.child("kodKursu").getValue().toString()).child("nazwa").getValue().toString().trim()
                                    + ", " + grupa.child("dzienTyg").getValue().toString().trim()
                                    + " " + grupa.child("godzRoz").getValue().toString().trim();
                            break;
                        }
                    }
                }

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    //Zapisywanie na NFC


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

        kod = text;
        if(butObecnosc.isEnabled()){
            zapiszObecnosc();

        }
    }



    private void zapiszObecnosc(){
        database.child("Kody").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                String kodObecnosci = dataSnapshot.child(idGrupy.toString()).child("kod").getValue().toString().trim();
                if(kodObecnosci.equals(kod)){
                    database.child("Obecnosci").child(idGrupy.toString()).child(nrAlbumu.toString()).setValue("Obecny")
                            .addOnCompleteListener(new OnCompleteListener<Void>() {
                                @Override
                                public void onComplete(@NonNull Task<Void> task) {
                                    if(task.isSuccessful())
                                        Toast.makeText(PresenceActivity.this, "Twoja obecność została odnotowana", Toast.LENGTH_LONG).show();
                                    else
                                        Toast.makeText(PresenceActivity.this, "Wystąpił błąd", Toast.LENGTH_LONG).show();
                                }
                            });
                }

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
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
