package com.example.trackerwydatkow;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.trackerwydatkow.wydatki.WydatkiBaza;
import com.example.trackerwydatkow.wydatki.Wydatki;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    Button btnStatystyki, btnPoprzednie, btnDodajWydatek;
    private WydatkiBaza database;
    private TextView textKwota30;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        textKwota30 = findViewById(R.id.textKwota30);
        database = WydatkiBaza.getInstance(this);
        aktualizujWydatki();
        Button btnDodajWydatek = findViewById(R.id.btnDodajWydatek);
        btnPoprzednie = findViewById(R.id.btnPoprzednie);
        btnDodajWydatek.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, DodajWydatek.class);
            startActivity(intent);
        });
        btnPoprzednie.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, PoprzednieWydatki.class);
            startActivity(intent);
        });

    }

    private void aktualizujWydatki() {
        new Thread(() -> {
            Calendar calendar = Calendar.getInstance();
            calendar.add(Calendar.DAY_OF_MONTH, -30);
            String startDate = new SimpleDateFormat("yyyy-MM-dd",
                    Locale.getDefault()).format(calendar.getTime());
            double ostatnie30 = database.DAOWydatki().getTotalExpensesAfterDate(startDate);
            runOnUiThread(() -> {
                String message = String.format(Locale.getDefault(),
                        "%.2f zł", ostatnie30);
                textKwota30.setText(message);
            });
        }).start();
    }
    protected void onResume() {
        super.onResume();
        aktualizujWydatki();
    }
}