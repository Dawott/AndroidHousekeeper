package com.example.trackerwydatkow;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
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

import java.text.SimpleDateFormat;
import java.util.*;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class Statystyki extends AppCompatActivity {
    private WydatkiBaza database;
    private Spinner spinnerFromCurrency, spinnerToCurrency;
    private EditText editAmount;
    private Button btnConvert;
    private TextView textConvertedAmount;

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
        setupCurrencyConverter();
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
                double totalAmount = 0;
                for (Wydatki expense : allExpenses) {
                    totalAmount += expense.getKwota();
                }

                Wydatki mostExpensive = Collections.max(allExpenses,
                        Comparator.comparing(Wydatki::getKwota));

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

                Set<String> uniqueCategories = new HashSet<>();
                for (Wydatki expense : allExpenses) {
                    uniqueCategories.add(expense.getKategoria());
                }

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
        editAmount = findViewById(R.id.editAmount);
        btnConvert = findViewById(R.id.btnConvert);
        textConvertedAmount = findViewById(R.id.textConvertedAmount);
        spinnerFromCurrency = findViewById(R.id.spinnerFromCurrency);
        spinnerToCurrency = findViewById(R.id.spinnerToCurrency);

        String[] currencies = {"PLN", "EUR", "USD", "GBP", "CHF", "CZK", "NOK", "SEK", "DKK"};

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, currencies);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        spinnerFromCurrency.setAdapter(adapter);
        spinnerToCurrency.setAdapter(adapter);

        btnConvert.setOnClickListener(v -> {
            String amountStr = editAmount.getText().toString().trim();
            if (amountStr.isEmpty()) {
                Toast.makeText(this, "Wprowadź kwotę", Toast.LENGTH_SHORT).show();
                return;
            }

            try {
                double amount = Double.parseDouble(amountStr);
                String fromCurrency = spinnerFromCurrency.getSelectedItem().toString();
                String toCurrency = spinnerToCurrency.getSelectedItem().toString();

                if (fromCurrency.equals(toCurrency)) {
                    textConvertedAmount.setText(String.format("%.2f %s = %.2f %s",
                            amount, fromCurrency, amount, toCurrency));
                    return;
                }

                convertCurrency(amount, fromCurrency, toCurrency);
            } catch (NumberFormatException e) {
                Toast.makeText(this, "Nieprawidłowa kwota", Toast.LENGTH_SHORT).show();
            }

        });
    }

    private void convertCurrency(double amount, String fromCurrency, String toCurrency) {
        textConvertedAmount.setText("Konwertowanie...");

        CurrencyService.getAPI().getExchangeRates(fromCurrency).enqueue(new Callback<CurrencyResponse>() {
            @Override
            public void onResponse(Call<CurrencyResponse> call, Response<CurrencyResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Map<String, Double> rates = response.body().getRates();

                    if (rates.containsKey(toCurrency)) {
                        double exchangeRate = rates.get(toCurrency);
                        double convertedAmount = amount * exchangeRate;

                        runOnUiThread(() -> {
                            textConvertedAmount.setText(String.format(Locale.getDefault(),
                                    "%.2f %s = %.2f %s", amount, fromCurrency, convertedAmount, toCurrency));
                        });
                    } else {
                        runOnUiThread(() -> {
                            textConvertedAmount.setText("Waluta " + toCurrency + " nie jest dostępna");
                        });
                    }
                } else {
                    runOnUiThread(() -> {
                        textConvertedAmount.setText("Błąd podczas pobierania kursu");
                    });
                }
            }

            @Override
            public void onFailure(Call<CurrencyResponse> call, Throwable t) {
                runOnUiThread(() -> {
                    textConvertedAmount.setText("Błąd połączenia z internetem");
                    Toast.makeText(Statystyki.this, "Sprawdź połączenie internetowe", Toast.LENGTH_SHORT).show();
                });
            }
        });
    }
}