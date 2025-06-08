package com.example.trackerwydatkow;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.trackerwydatkow.wydatki.Wydatki;
import com.example.trackerwydatkow.wydatki.WydatkiBaza;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class DodajWydatek extends AppCompatActivity {
    private MaterialAutoCompleteTextView dropdown;
    private ArrayAdapter<String> adapter;
    private EditText editDodajWydatek;
    private EditText editTextNumberDecimal;
    private Button btnDodajWpis;
    private Button btnPowrot;
    private WydatkiBaza database;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_dodaj_wydatek);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;


        });
        dropdown = findViewById(R.id.dropdown);
        btnDodajWpis = findViewById(R.id.btnDodajWpis);
        btnPowrot = findViewById(R.id.btnPowrot);
        editDodajWydatek = findViewById(R.id.editDodajWydatek);
        editTextNumberDecimal = findViewById(R.id.editTextNumberDecimal);
        database = WydatkiBaza.getInstance(this);
        btnDodajWpis.setOnClickListener(v -> zapiszWydatek());

        btnPowrot.setOnClickListener(v -> finish());

        String[] kategorie = new String[]{
                "Jedzenie",
                "Rachunki",
                "Paliwo",
                "Rozrywki",
                "Zakupy",
                "Zdrowie"
        };
        adapter = new ArrayAdapter<>(
                this,
                R.layout.dropdown_item,
                kategorie
        );
        dropdown.setAdapter(adapter);
        dropdown.setOnItemClickListener((parent, view, position, id) -> {
            String selectedCategory = kategorie[position];
            Toast.makeText(this, "Wybrano: " + selectedCategory, Toast.LENGTH_SHORT).show();
        });


    }

    private void zapiszWydatek() {
        String nazwa = editDodajWydatek.getText().toString().trim();
        String wydatek = editTextNumberDecimal.getText().toString().trim();
        String kategoria = dropdown.getText().toString().trim();
        if (nazwa.isEmpty() || wydatek.isEmpty() || kategoria.isEmpty()) {
            Toast.makeText(this, "Puste pola!", Toast.LENGTH_SHORT).show();
            return;
        }
        double kwotaWydatku;
        try {
            kwotaWydatku = Double.parseDouble(wydatek);
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Niewłaściwa kwota", Toast.LENGTH_SHORT).show();
            return;
        }
        String currentDate = new SimpleDateFormat("yyyy-MM-dd",
                Locale.getDefault()).format(new Date());
        Wydatki wydatki = new Wydatki(nazwa, kwotaWydatku, kategoria, currentDate);

        new Thread(() -> {
            database.DAOWydatki().insert(wydatki);
            runOnUiThread(() -> {
                Toast.makeText(DodajWydatek.this,
                        "Dodano poprawnie", Toast.LENGTH_SHORT).show();

                Intent resultIntent = new Intent();
                setResult(Activity.RESULT_OK, resultIntent);
                finish();
            });
        }).start();



    }
}