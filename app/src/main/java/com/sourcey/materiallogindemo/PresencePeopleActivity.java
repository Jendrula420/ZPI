package com.sourcey.materiallogindemo;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import java.util.ArrayList;

public class PresencePeopleActivity extends AppCompatActivity {

    ListView listaObecnosciView;
    ArrayList<String> lista;
    ArrayAdapter<String> adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_presence_people);

        listaObecnosciView = findViewById(R.id.list_listaObecnosci);
        lista = (ArrayList<String>) getIntent().getSerializableExtra("Lista");
        adapter = new ArrayAdapter<String>(this, R.layout.presence_list_item, lista);
        listaObecnosciView.setAdapter(adapter);
    }
}
