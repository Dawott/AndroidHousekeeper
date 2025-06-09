package com.example.trackerwydatkow;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.trackerwydatkow.api.CurrencyResponse;
import com.example.trackerwydatkow.api.CurrencyService;
import com.example.trackerwydatkow.wydatki.WydatkiBaza;
import com.example.trackerwydatkow.wydatki.Wydatki;

import java.text.SimpleDateFormat;
import java.util.*;

public class Statystyki extends AppCompatActivity {
    private WydatkiBaza database;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_statystyki);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        database = WydatkiBaza.getInstance(this);

        createBasicStats();
    }

    private void createBasicStats() {
        TextView textMostExpensive = findViewById(R.id.textMostExpensive);
        TextView textAverageDaily = findViewById(R.id.textAverageDaily);
        TextView textTotalCategories = findViewById(R.id.textTotalCategories);

        loadBasicStatistics(textMostExpensive, textAverageDaily, textTotalCategories);
    }

    private void loadBasicStatistics(TextView textMostExpensive, TextView textAverageDaily, TextView textTotalCategories) {
        new Thread(() -> {
            List<Wydatki> allExpenses = database.DAOWydatki().getAllExpenses();

            if (allExpenses.isEmpty()) {
                runOnUiThread(() -> {
                    textMostExpensive.setText("Najdroższy wydatek: Brak danych");
                    textAverageDaily.setText("Średnia dzienna: Brak danych");
                    textTotalCategories.setText("Kategorie: Brak danych");
                });
            } else {
                // Calculate statistics
                double totalAmount = 0;
                for (Wydatki expense : allExpenses) {
                    totalAmount += expense.getKwota();
                }

                // Find most expensive
                Wydatki mostExpensive = Collections.max(allExpenses,
                        Comparator.comparing(Wydatki::getKwota));

                // Calculate average daily (last 30 days)
                Calendar cal = Calendar.getInstance();
                cal.add(Calendar.DAY_OF_MONTH, -30);
                String thirtyDaysAgo = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                        .format(cal.getTime());

                double last30Days = 0;
                int count30Days = 0;
                for (Wydatki expense : allExpenses) {
                    if (expense.getData().compareTo(thirtyDaysAgo) >= 0) {
                        last30Days += expense.getKwota();
                        count30Days++;
                    }
                }
                double averageDaily = last30Days / 30;

                // Count categories
                Set<String> uniqueCategories = new HashSet<>();
                for (Wydatki expense : allExpenses) {
                    uniqueCategories.add(expense.getKategoria());
                }

                // Update UI
                runOnUiThread(() -> {
                    textMostExpensive.setText(String.format("Najdroższy wydatek: %s (%.2f zł)",
                            mostExpensive.getNazwa(), mostExpensive.getKwota()));
                    textAverageDaily.setText(String.format("Średnia dzienna: %.2f zł", averageDaily));
                    textTotalCategories.setText(String.format("Kategorie: %d używanych", uniqueCategories.size()));
                });
            }
        }).start();
    }
    private void setupCurrencyConverter() {
        EditText editAmount = findViewById(R.id.editAmount);
        Button btnConvert = findViewById(R.id.btnConvert);
        TextView textConvertedAmount = findViewById(R.id.textConvertedAmount);

        btnConvert.setOnClickListener(v -> {
            String amountStr = editAmount.getText().toString();
            if (!amountStr.isEmpty()) {
                double amount = Double.parseDouble(amountStr);
                convertCurrency(amount, textConvertedAmount);
            }
        });
    }

    private void convertCurrency(double amount, TextView resultText) {
        CurrencyService.getAPI().getExchangeRates().enqueue(new retrofit2.Callback<CurrencyResponse>() {
            @Override
            public void onResponse(retrofit2.Call<CurrencyResponse> call, retrofit2.Response<CurrencyResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Map<String, Double> rates = response.body().getRates();
                    if (rates.containsKey("EUR")) {
                        double eurRate = rates.get("EUR");
                        double convertedAmount = amount * eurRate;
                        resultText.setText(String.format("%.2f PLN = %.2f EUR", amount, convertedAmount));
                    }
                }
            }

            @Override
            public void onFailure(retrofit2.Call<CurrencyResponse> call, Throwable t) {
                resultText.setText("Błąd podczas pobierania kursu");
            }
        });
    }

}