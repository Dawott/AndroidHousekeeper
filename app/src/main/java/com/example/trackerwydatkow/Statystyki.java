package com.example.trackerwydatkow;

import android.os.Bundle;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

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
        TextView statsText = new TextView(this);
        statsText.setTextSize(16);
        statsText.setPadding(32, 32, 32, 32);
        statsText.setText("Ładowanie statystyk...");

        androidx.constraintlayout.widget.ConstraintLayout mainLayout = findViewById(R.id.main);
        androidx.constraintlayout.widget.ConstraintLayout.LayoutParams params =
                new androidx.constraintlayout.widget.ConstraintLayout.LayoutParams(
                        androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.MATCH_PARENT,
                        androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.WRAP_CONTENT
                );

        params.topToTop = androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.PARENT_ID;
        params.leftToLeft = androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.PARENT_ID;
        params.rightToRight = androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.PARENT_ID;
        params.topMargin = 100;

        statsText.setLayoutParams(params);
        mainLayout.addView(statsText);

        loadBasicStatistics(statsText);
    }

    private void loadBasicStatistics(TextView statsText) {
        new Thread(() -> {
            List<Wydatki> allExpenses = database.DAOWydatki().getAllExpenses();

            StringBuilder stats = new StringBuilder();
            stats.append("📊 STATYSTYKI WYDATKÓW\n\n");

            if (allExpenses.isEmpty()) {
                stats.append("Brak wydatków do wyświetlenia.");
            } else {
                double totalAmount = 0;
                for (Wydatki expense : allExpenses) {
                    totalAmount += expense.getKwota();
                }
                stats.append("💰 Łączne wydatki: ").append(String.format("%.2f zł", totalAmount)).append("\n\n");

                Map<String, Integer> categoryCount = new HashMap<>();
                Map<String, Double> categoryAmount = new HashMap<>();

                for (Wydatki expense : allExpenses) {
                    String category = expense.getKategoria();
                    categoryCount.put(category, categoryCount.getOrDefault(category, 0) + 1);
                    categoryAmount.put(category, categoryAmount.getOrDefault(category, 0.0) + expense.getKwota());
                }

                stats.append("📂 Wydatki według kategorii:\n");
                for (Map.Entry<String, Double> entry : categoryAmount.entrySet()) {
                    String category = entry.getKey();
                    double amount = entry.getValue();
                    int count = categoryCount.get(category);
                    stats.append("• ").append(category).append(": ").append(String.format("%.2f zł", amount))
                            .append(" (").append(count).append(" wydatków)\n");
                }

                Wydatki mostExpensive = Collections.max(allExpenses,
                        Comparator.comparing(Wydatki::getKwota));
                stats.append("\n🏆 Najdroższy wydatek:\n");
                stats.append("• ").append(mostExpensive.getNazwa()).append(": ")
                        .append(String.format("%.2f zł", mostExpensive.getKwota())).append("\n");

                double average = totalAmount / allExpenses.size();
                stats.append("\n📈 Średni wydatek: ").append(String.format("%.2f zł", average)).append("\n");

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

                stats.append("\n📅 Ostatnie 30 dni:\n");
                stats.append("• Kwota: ").append(String.format("%.2f zł", last30Days)).append("\n");
                stats.append("• Liczba wydatków: ").append(count30Days).append("\n");
            }

            runOnUiThread(() -> statsText.setText(stats.toString()));
        }).start();
    }
}