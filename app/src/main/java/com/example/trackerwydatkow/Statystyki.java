package com.example.trackerwydatkow;

import static android.content.ContentValues.TAG;

import android.graphics.Color;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.util.Log;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.trackerwydatkow.api.CurrencyResponse;
import com.example.trackerwydatkow.api.CurrencyService;
import com.example.trackerwydatkow.wydatki.WydatkiBaza;
import com.example.trackerwydatkow.wydatki.Wydatki;

// Import dla wykresów
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;

import java.text.SimpleDateFormat;
import java.text.ParseException;
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

    // Dodane referencje do wykresów
    private PieChart pieChart;
    private LineChart lineChart;

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

        // Inicjalizacja wykresów
        initializeCharts();

        createBasicStats();
        setupCurrencyConverter();

        // Załaduj dane do wykresów
        loadChartsData();
    }

    private void initializeCharts() {
        pieChart = findViewById(R.id.pieChart);
        lineChart = findViewById(R.id.lineChart);

        setupPieChart();
        setupLineChart();
    }

    private void setupPieChart() {
        pieChart.setUsePercentValues(true);
        pieChart.getDescription().setEnabled(false);
        pieChart.setExtraOffsets(5, 10, 5, 5);

        pieChart.setDragDecelerationFrictionCoef(0.95f);
        pieChart.setDrawHoleEnabled(true);
        pieChart.setHoleColor(Color.WHITE);
        pieChart.setTransparentCircleRadius(61f);

        // Ustawienia legendy
        Legend legend = pieChart.getLegend();
        legend.setVerticalAlignment(Legend.LegendVerticalAlignment.TOP);
        legend.setHorizontalAlignment(Legend.LegendHorizontalAlignment.RIGHT);
        legend.setOrientation(Legend.LegendOrientation.VERTICAL);
        legend.setDrawInside(false);
        legend.setXEntrySpace(7f);
        legend.setYEntrySpace(0f);
        legend.setYOffset(0f);
    }

    private void setupLineChart() {
        lineChart.getDescription().setEnabled(false);
        lineChart.setTouchEnabled(true);
        lineChart.setDragEnabled(true);
        lineChart.setScaleEnabled(true);
        lineChart.setPinchZoom(true);

        // Oś X
        XAxis xAxis = lineChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setGranularity(1f);
        xAxis.setLabelCount(7);

        // Oś Y lewa
        YAxis leftAxis = lineChart.getAxisLeft();
        leftAxis.setDrawGridLines(true);
        leftAxis.setAxisMinimum(0f);

        // Oś Y prawa - wyłączona
        lineChart.getAxisRight().setEnabled(false);

        // Legenda
        Legend legend = lineChart.getLegend();
        legend.setForm(Legend.LegendForm.LINE);
        legend.setTextSize(11f);
        legend.setVerticalAlignment(Legend.LegendVerticalAlignment.BOTTOM);
        legend.setHorizontalAlignment(Legend.LegendHorizontalAlignment.LEFT);
        legend.setOrientation(Legend.LegendOrientation.HORIZONTAL);
        legend.setDrawInside(false);
    }

    private void loadChartsData() {
        new Thread(() -> {
            List<Wydatki> allExpenses = database.DAOWydatki().getAllExpenses();

            runOnUiThread(() -> {
                loadPieChartData(allExpenses);
                loadLineChartData(allExpenses);
            });
        }).start();
    }

    private void loadPieChartData(List<Wydatki> expenses) {
        Map<String, Float> categoryTotals = new HashMap<>();

        // Oblicz sumy dla każdej kategorii
        for (Wydatki expense : expenses) {
            String category = expense.getKategoria();
            float amount = (float) expense.getKwota();
            categoryTotals.put(category, categoryTotals.getOrDefault(category, 0f) + amount);
        }

        // Utwórz dane dla wykresu kołowego
        ArrayList<PieEntry> entries = new ArrayList<>();
        for (Map.Entry<String, Float> entry : categoryTotals.entrySet()) {
            entries.add(new PieEntry(entry.getValue(), entry.getKey()));
        }

        if (entries.isEmpty()) {
            pieChart.setNoDataText("Brak danych do wyświetlenia");
            return;
        }

        PieDataSet dataSet = new PieDataSet(entries, "Wydatki według kategorii");
        dataSet.setSliceSpace(3f);
        dataSet.setSelectionShift(5f);

        // Kolory dla kategorii
        ArrayList<Integer> colors = new ArrayList<>();
        colors.add(Color.rgb(64, 89, 128));
        colors.add(Color.rgb(149, 165, 124));
        colors.add(Color.rgb(217, 184, 162));
        colors.add(Color.rgb(191, 134, 134));
        colors.add(Color.rgb(179, 48, 80));
        colors.add(Color.rgb(193, 37, 82));
        dataSet.setColors(colors);

        PieData data = new PieData(dataSet);
        data.setValueTextSize(10f);
        data.setValueTextColor(Color.WHITE);

        pieChart.setData(data);
        pieChart.invalidate();
    }

    private void loadLineChartData(List<Wydatki> expenses) {
        // Pobierz wydatki z ostatnich 30 dni
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_MONTH, -30);
        String startDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.getTime());

        // Filtruj wydatki z ostatnich 30 dni
        List<Wydatki> recentExpenses = new ArrayList<>();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

        for (Wydatki expense : expenses) {
            try {
                Date expenseDate = sdf.parse(expense.getData());
                Date startDateParsed = sdf.parse(startDate);
                if (expenseDate != null && startDateParsed != null && expenseDate.after(startDateParsed)) {
                    recentExpenses.add(expense);
                }
            } catch (ParseException e) {
                Log.e(TAG, "Błąd parsowania daty: " + expense.getData());
            }
        }

        // Grupuj wydatki według dni
        Map<String, Float> dailyTotals = new TreeMap<>();

        // Inicjalizuj wszystkie dni ostatnich 30 dni z zerowymi wartościami
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_MONTH, -29); // Zaczynamy od 29 dni temu
        for (int i = 0; i < 30; i++) {
            String dateKey = sdf.format(cal.getTime());
            dailyTotals.put(dateKey, 0f);
            cal.add(Calendar.DAY_OF_MONTH, 1);
        }

        // Dodaj rzeczywiste wydatki
        for (Wydatki expense : recentExpenses) {
            String dateKey = expense.getData();
            float amount = (float) expense.getKwota();
            dailyTotals.put(dateKey, dailyTotals.getOrDefault(dateKey, 0f) + amount);
        }

        // Utwórz dane dla wykresu liniowego
        ArrayList<Entry> entries = new ArrayList<>();
        ArrayList<String> dates = new ArrayList<>();
        int index = 0;

        for (Map.Entry<String, Float> entry : dailyTotals.entrySet()) {
            entries.add(new Entry(index, entry.getValue()));

            // Formatuj datę do wyświetlenia (tylko dzień i miesiąc)
            try {
                Date date = sdf.parse(entry.getKey());
                SimpleDateFormat displayFormat = new SimpleDateFormat("dd/MM", Locale.getDefault());
                dates.add(displayFormat.format(date));
            } catch (ParseException e) {
                dates.add(entry.getKey());
            }
            index++;
        }

        if (entries.isEmpty()) {
            lineChart.setNoDataText("Brak danych z ostatnich 30 dni");
            return;
        }

        LineDataSet dataSet = new LineDataSet(entries, "Wydatki dzienne (PLN)");
        dataSet.setColor(Color.rgb(64, 89, 128));
        dataSet.setCircleColor(Color.rgb(64, 89, 128));
        dataSet.setLineWidth(2f);
        dataSet.setCircleRadius(4f);
        dataSet.setDrawCircleHole(false);
        dataSet.setValueTextSize(10f);
        dataSet.setDrawFilled(true);
        dataSet.setFillColor(Color.rgb(64, 89, 128));
        dataSet.setFillAlpha(30);

        LineData data = new LineData(dataSet);

        // Ustaw etykiety osi X
        XAxis xAxis = lineChart.getXAxis();
        xAxis.setValueFormatter(new IndexAxisValueFormatter(dates));
        xAxis.setLabelRotationAngle(-45);

        lineChart.setData(data);
        lineChart.invalidate();
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
        Log.d(TAG, "Odpalanie metody setupCurrencyConverter...");

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

        spinnerFromCurrency.setSelection(0); // PLN
        spinnerToCurrency.setSelection(1); // EUR

        btnConvert.setOnClickListener(v -> {
            Log.d(TAG, "Kliknięta konwersja");
            String amountStr = editAmount.getText().toString().trim();
            if (amountStr.isEmpty()) {
                Toast.makeText(this, "Wprowadź kwotę", Toast.LENGTH_SHORT).show();
                Log.w(TAG, "Pusta wartość kwoty?");
                return;
            }

            try {
                double amount = Double.parseDouble(amountStr);
                String fromCurrency = spinnerFromCurrency.getSelectedItem().toString();
                String toCurrency = spinnerToCurrency.getSelectedItem().toString();
                Log.d(TAG, String.format("Converting %.2f from %s to %s", amount, fromCurrency, toCurrency));
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
                        Log.e(TAG, "Błąd API: " + response.code() +
                                ", Message: " + response.message());
                        textConvertedAmount.setText("Błąd podczas pobierania kursu");
                    });
                }
            }

            @Override
            public void onFailure(Call<CurrencyResponse> call, Throwable t) {
                Log.e(TAG, "API call upadł", t);
                runOnUiThread(() -> {
                    textConvertedAmount.setText("Błąd połączenia z internetem");
                    Toast.makeText(Statystyki.this, "Sprawdź połączenie internetowe", Toast.LENGTH_SHORT).show();
                });
            }
        });
    }
}