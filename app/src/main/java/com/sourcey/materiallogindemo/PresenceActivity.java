package com.sourcey.materiallogindemo;

import android.content.Intent;
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

import org.w3c.dom.Text;

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
    Spinner spinnerProwadzacy;
    ArrayAdapter<String> spinnerAdapter;
    ArrayList<String[]> daneKursow; //[0]idProw, [1]idGrupy, [2]kodKursu, [3]nazwaKursu, [4]dzienTyg, [5]godzRoz, [6]godzZak

    ArrayList<String> elementySpinner;
    ArrayList<String> prowadzacyList;
    String nrAlbumu;
    String daneStudent;
    String wybranyProw;
    String wybranyKurs;
    String idGrupy;

    FirebaseAuth firebaseAuth;
    DatabaseReference database;



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
        spinnerProwadzacy = findViewById(R.id.spinner_prowadzacy);

        daneKursow = new ArrayList<>();
        elementySpinner = new ArrayList<>();
        prowadzacyList = new ArrayList<>();
        firebaseAuth = FirebaseAuth.getInstance();
        database = FirebaseDatabase.getInstance().getReference();

        ustalIdStudent();




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
                else
                    textKurs.setText(wybranyKurs);

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


}
