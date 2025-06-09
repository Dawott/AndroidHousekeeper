package com.example.trackerwydatkow;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
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
    private LocationManager locationManager;
    private String currentLocation = "";
    private Spinner spinnerWaluta;

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
        spinnerWaluta = findViewById(R.id.spinnerWaluta);

        btnPowrot.setOnClickListener(v -> finish());

        setupCategoryDropdown();
        setupCurrencySpinner();
    }

        private void setupCategoryDropdown() {
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

    private void setupCurrencySpinner() {
        String[] currencies = {"PLN", "EUR", "USD", "GBP", "CHF", "CZK", "NOK", "SEK", "DKK"};

        ArrayAdapter<String> currencyAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, currencies);
        currencyAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        spinnerWaluta.setAdapter(currencyAdapter);
        spinnerWaluta.setSelection(0);
    }


    private void zapiszWydatek() {
        String nazwa = editDodajWydatek.getText().toString().trim();
        String wydatek = editTextNumberDecimal.getText().toString().trim();
        String kategoria = dropdown.getText().toString().trim();
        String waluta = spinnerWaluta.getSelectedItem().toString();

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
        Wydatki wydatki = new Wydatki(nazwa, kwotaWydatku, kategoria, currentDate, waluta);

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
/*
    private void getCurrentLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
            return;
        }

        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        Location location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);

        if (location != null) {
            getNearbyPlaces(location.getLatitude(), location.getLongitude());
        }
    }

    private void getNearbyPlaces(double lat, double lng) {
        String url = "https://maps.googleapis.com/maps/api/place/nearbysearch/json?" +
                "location=" + lat + "," + lng +
                "&radius=100&type=establishment&key=" + PLACES_API_KEY;

        PlacesService.getAPI().getNearbyPlaces(url).enqueue(new Callback<PlacesResponse>() {
            @Override
            public void onResponse(Call<PlacesResponse> call, Response<PlacesResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<Place> places = response.body().getResults();
                    if (!places.isEmpty()) {
                        currentLocation = places.get(0).getName();
                        editDodajWydatek.setText(currentLocation);
                    }
                }
            }

            @Override
            public void onFailure(Call<PlacesResponse> call, Throwable t) {
                currentLocation = "Lokacja nieznana";
            }
        });
    }*/
}