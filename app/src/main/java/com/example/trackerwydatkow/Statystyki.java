package com.example.trackerwydatkow;

import android.graphics.Color;
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
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.data.Entry;

import java.text.SimpleDateFormat;
import java.util.*;

public class Statystyki extends AppCompatActivity {
    private WydatkiBaza database;
    private PieChart pieChart;
    private LineChart lineChart;
    private TextView textMostExpensive, textAverageDaily, textTotalCategories;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_statystyki);

        database = WydatkiBaza.getInstance(this);

        pieChart = findViewById(R.id.pieChart);
        lineChart = findViewById(R.id.lineChart);
        textMostExpensive = findViewById(R.id.textMostExpensive);
        textAverageDaily = findViewById(R.id.textAverageDaily);
        textTotalCategories = findViewById(R.id.textTotalCategories);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        loadStatistics();
    }

    private void loadStatistics() {
        new Thread(() -> {
            List<Wydatki> allExpenses = database.DAOWydatki().getAllExpenses();

            runOnUiThread(() -> {
                setupPieChart(allExpenses);
                setupLineChart(allExpenses);
                calculateBasicStats(allExpenses);
            });
        }).start();
    }

    private void setupPieChart(List<Wydatki> expenses) {
        Map<String, Double> categoryTotals = new HashMap<>();
        for (Wydatki expense : expenses) {
            categoryTotals.put(expense.getKategoria(),
                    categoryTotals.getOrDefault(expense.getKategoria(), 0.0) + expense.getKwota());
        }

        List<PieEntry> entries = new ArrayList<>();
        for (Map.Entry<String, Double> entry : categoryTotals.entrySet()) {
            entries.add(new PieEntry(entry.getValue().floatValue(), entry.getKey()));
        }

        PieDataSet dataSet = new PieDataSet(entries, "Wydatki według kategorii");
        dataSet.setColors(new int[]{
                Color.rgb(255, 102, 102), // Red
                Color.rgb(102, 178, 255), // Blue
                Color.rgb(255, 204, 102), // Yellow
                Color.rgb(102, 255, 178), // Green
                Color.rgb(204, 102, 255), // Purple
                Color.rgb(255, 178, 102)  // Orange
        });

        PieData pieData = new PieData(dataSet);
        pieChart.setData(pieData);
        pieChart.getDescription().setEnabled(false);
        pieChart.invalidate();
    }

    private void setupLineChart(List<Wydatki> expenses) {
        Map<String, Double> dailyTotals = new HashMap<>();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

        for (Wydatki expense : expenses) {
            String date = expense.getData();
            dailyTotals.put(date, dailyTotals.getOrDefault(date, 0.0) + expense.getKwota());
        }

        List<Entry> entries = new ArrayList<>();
        List<String> sortedDates = new ArrayList<>(dailyTotals.keySet());
        Collections.sort(sortedDates);

        for (int i = 0; i < sortedDates.size() && i < 30; i++) {
            String date = sortedDates.get(sortedDates.size() - 30 + i);
            if (date != null && dailyTotals.containsKey(date)) {
                entries.add(new Entry(i, dailyTotals.get(date).floatValue()));
            }
        }

        LineDataSet dataSet = new LineDataSet(entries, "Wydatki dzienne (ostatnie 30 dni)");
        dataSet.setColor(Color.rgb(102, 178, 255));
        dataSet.setValueTextColor(Color.BLACK);

        LineData lineData = new LineData(dataSet);
        lineChart.setData(lineData);
        lineChart.getDescription().setEnabled(false);
        lineChart.invalidate();
    }

    private void calculateBasicStats(List<Wydatki> expenses) {
        if (expenses.isEmpty()) {
            textMostExpensive.setText("Najdroższy wydatek: Brak danych");
            textAverageDaily.setText("Średnia dzienna: 0 zł");
            textTotalCategories.setText("Kategorie: 0");
            return;
        }

        Wydatki mostExpensive = Collections.max(expenses,
                Comparator.comparing(Wydatki::getKwota));

        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_MONTH, -30);
        String thirtyDaysAgo = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                .format(cal.getTime());

        double totalLast30Days = 0;
        int daysWithExpenses = 0;
        Set<String> datesWithExpenses = new HashSet<>();

        for (Wydatki expense : expenses) {
            if (expense.getData().compareTo(thirtyDaysAgo) >= 0) {
                totalLast30Days += expense.getKwota();
                datesWithExpenses.add(expense.getData());
            }
        }

        double dailyAverage = datesWithExpenses.isEmpty() ? 0 :
                totalLast30Days / Math.max(1, datesWithExpenses.size());

        Set<String> uniqueCategories = new HashSet<>();
        for (Wydatki expense : expenses) {
            uniqueCategories.add(expense.getKategoria());
        }

        textMostExpensive.setText(String.format("Najdroższy: %s (%.2f zł)",
                mostExpensive.getNazwa(), mostExpensive.getKwota()));
        textAverageDaily.setText(String.format("Średnia dzienna: %.2f zł", dailyAverage));
        textTotalCategories.setText(String.format("Kategorie: %d", uniqueCategories.size()));
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