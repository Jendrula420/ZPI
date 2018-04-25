package com.sourcey.materiallogindemo;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
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

import java.util.ArrayList;
import java.util.Iterator;

import butterknife.BindView;

public class GenerateKeyActivity extends AppCompatActivity {

    TextView textProwadzacy;
    TextView textWybor;
    TextView textKod;
    Button butWyloguj;
    Button butGenerujKod;
    Button butZapisz;
    Button butOdswiez;
    Spinner spinnerKursy;
    ArrayAdapter<String> spinnerAdapter;
    ArrayList<String[]> daneKursow; //[0]nazwKursu, [1]idGrupy, [2]kodKursu, [3]dzienTyg, [4]godzRozp

    String idProwadzacy;
    String idGrupa;
    String[] elementySpinner;

    FirebaseAuth firebaseAuth;
    DatabaseReference database;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_generate_key);

        textKod = findViewById(R.id.text_kod);
        textProwadzacy = findViewById(R.id.text_prowadzacy);
        textWybor = findViewById(R.id.text_wybor);
        spinnerKursy = findViewById(R.id.spinner_kursy);
        butGenerujKod = findViewById(R.id.but_generuj);
        butOdswiez = findViewById(R.id.but_odswiez);

        firebaseAuth = FirebaseAuth.getInstance();
        database = FirebaseDatabase.getInstance().getReference();
        daneKursow = new ArrayList<>();


        ustalIdProwadzacy();


        butOdswiez.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                odswiezSpinner();
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
                    if(grupa.child("idProw").getValue().equals(idProwadzacy)){
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
}
