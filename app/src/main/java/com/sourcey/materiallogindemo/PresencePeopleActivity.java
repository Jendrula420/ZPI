package com.sourcey.materiallogindemo;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collections;

public class PresencePeopleActivity extends AppCompatActivity {

    ListView listaObecnosciView;
    TextView nazwaZajec;

    ArrayList<String> lista;
    String zajecia;
    ArrayAdapter<String> adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_presence_people);

        listaObecnosciView = findViewById(R.id.list_listaObecnosci);
        nazwaZajec = findViewById(R.id.id_text_classes);

        lista = (ArrayList<String>) getIntent().getSerializableExtra("Lista");
        zajecia = getIntent().getStringExtra("Nazwa zajec");
        Collections.sort(lista);
        adapter = new ArrayAdapter<String>(this, R.layout.presence_list_item, lista);
        listaObecnosciView.setAdapter(adapter);

        nazwaZajec.setText(zajecia);
    }
}
