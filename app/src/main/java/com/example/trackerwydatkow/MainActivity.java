package com.example.trackerwydatkow;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.trackerwydatkow.api.CurrencyResponse;
import com.example.trackerwydatkow.api.CurrencyService;
import com.example.trackerwydatkow.wydatki.WydatkiBaza;
import com.example.trackerwydatkow.wydatki.Wydatki;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

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
        btnDodajWydatek = findViewById(R.id.btnDodajWydatek);
        btnPoprzednie = findViewById(R.id.btnPoprzednie);
        btnStatystyki = findViewById(R.id.btnStatystyki);
        btnDodajWydatek.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, DodajWydatek.class);
            startActivity(intent);
        });
        btnPoprzednie.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, PoprzednieWydatki.class);
            startActivity(intent);
        });
        btnStatystyki.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, Statystyki.class);
            startActivity(intent);
        });

    }

    private void aktualizujWydatki() {
        textKwota30.setText("Liczenie...");

        new Thread(() -> {
            Calendar calendar = Calendar.getInstance();
            calendar.add(Calendar.DAY_OF_MONTH, -30);
            String startDate = new SimpleDateFormat("yyyy-MM-dd",
                    Locale.getDefault()).format(calendar.getTime());
            /*double ostatnie30 = database.DAOWydatki().getTotalExpensesAfterDate(startDate);
            runOnUiThread(() -> {
                String message = String.format(Locale.getDefault(),
                        "%.2f zł", ostatnie30);
                textKwota30.setText(message);*/
            List<Wydatki> ostatnie30Wydatki = database.DAOWydatki().getExpensesAfterDate(startDate);

            if (ostatnie30Wydatki.isEmpty()) {
                runOnUiThread(() -> textKwota30.setText("0.00 PLN"));
                return;
            }

            // Convert all expenses to PLN and sum them up
            convertExpensesToPLNAndSum(ostatnie30Wydatki);

        }).start();
    }

    private void convertExpensesToPLNAndSum(List<Wydatki> expenses) {
        AtomicReference<Double> totalInPLN = new AtomicReference<>(0.0);
        AtomicInteger pendingConversions = new AtomicInteger(0);

        Map<String, Double> currencyTotals = new HashMap<>();

        for (Wydatki expense : expenses) {
            String currency = expense.getWaluta();
            if (currency == null || currency.isEmpty()) {
                currency = "PLN";
            }
            currencyTotals.put(currency, currencyTotals.getOrDefault(currency, 0.0) + expense.getKwota());
        }

        for (Map.Entry<String, Double> entry : currencyTotals.entrySet()) {
            String currency = entry.getKey();
            Double amount = entry.getValue();

            if ("PLN".equals(currency)) {
                totalInPLN.updateAndGet(v -> v + amount);
            } else {
                pendingConversions.incrementAndGet();
                convertCurrencyToPLN(currency, amount, totalInPLN, pendingConversions, currencyTotals.size());
            }
        }

        if (pendingConversions.get() == 0) {
            runOnUiThread(() -> {
                String message = String.format(Locale.getDefault(), "%.2f PLN", totalInPLN.get());
                textKwota30.setText(message);
            });
        }
    }

    private void convertCurrencyToPLN(String fromCurrency, Double amount, AtomicReference<Double> totalInPLN,
                                      AtomicInteger pendingConversions, int totalCurrencies) {

        CurrencyService.getAPI().getExchangeRates(fromCurrency).enqueue(new Callback<CurrencyResponse>() {
            @Override
            public void onResponse(Call<CurrencyResponse> call, Response<CurrencyResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Map<String, Double> rates = response.body().getRates();
                    if (rates.containsKey("PLN")) {
                        double exchangeRate = rates.get("PLN");
                        double convertedAmount = amount * exchangeRate;
                        totalInPLN.updateAndGet(v -> v + convertedAmount);

                        Log.d("MainActivity", String.format("Converted %.2f %s to %.2f PLN",
                                amount, fromCurrency, convertedAmount));
                    }
                } else {
                    Log.e("MainActivity", "Failed to get exchange rate for " + fromCurrency);
                    // If conversion fails, add the amount as-is (not ideal but better than losing data)
                    totalInPLN.updateAndGet(v -> v + amount);
                }

                // Check if all conversions are done
                if (pendingConversions.decrementAndGet() == 0) {
                    runOnUiThread(() -> {
                        String message = String.format(Locale.getDefault(), "%.2f PLN", totalInPLN.get());
                        textKwota30.setText(message);
                    });
                }
            }

            @Override
            public void onFailure(Call<CurrencyResponse> call, Throwable t) {
                Log.e("MainActivity", "Network error converting " + fromCurrency, t);
                // If conversion fails, add the amount as-is
                totalInPLN.updateAndGet(v -> v + amount);

                if (pendingConversions.decrementAndGet() == 0) {
                    runOnUiThread(() -> {
                        String message = String.format(Locale.getDefault(), "%.2f PLN*", totalInPLN.get());
                        textKwota30.setText(message);
                        Toast.makeText(MainActivity.this, "Niektóre waluty nie zostały przeliczone", Toast.LENGTH_SHORT).show();
                    });
                }
            }
        });
    }

    protected void onResume() {
        super.onResume();
        aktualizujWydatki();
    }

    private void setupFirebaseSync() {
        Button btnSync = findViewById(R.id.btnSyncFirebase);
        btnSync.setOnClickListener(v -> syncWithFirebase());
    }

    private void syncWithFirebase() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        new Thread(() -> {
            List<Wydatki> localExpenses = database.DAOWydatki().getAllExpenses();

            for (Wydatki expense : localExpenses) {
                Map<String, Object> expenseData = new HashMap<>();
                expenseData.put("nazwa", expense.getNazwa());
                expenseData.put("kwota", expense.getKwota());
                expenseData.put("kategoria", expense.getKategoria());
                expenseData.put("data", expense.getData());
                expenseData.put("timestamp", System.currentTimeMillis());

                db.collection("expenses")
                        .add(expenseData)
                        .addOnSuccessListener(documentReference -> {
                            runOnUiThread(() ->
                                    Toast.makeText(this, "Zsynchronizowano z Firebase", Toast.LENGTH_SHORT).show());
                        })
                        .addOnFailureListener(e -> {
                            runOnUiThread(() ->
                                    Toast.makeText(this, "Błąd synchronizacji", Toast.LENGTH_SHORT).show());
                        });
            }
        }).start();
    }
}