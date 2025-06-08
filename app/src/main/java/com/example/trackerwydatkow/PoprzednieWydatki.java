package com.example.trackerwydatkow;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.trackerwydatkow.wydatki.Wydatki;
import com.example.trackerwydatkow.wydatki.WydatkiBaza;
import com.example.trackerwydatkow.AdapterWydatki;
import java.util.List;

public class PoprzednieWydatki extends AppCompatActivity {
    private AdapterWydatki adapter;
    private WydatkiBaza database;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_poprzednie_wydatki);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.linearMain), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        RecyclerView recyclerView = findViewById(R.id.recyclerWydatki);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setHasFixedSize(true);
        adapter = new AdapterWydatki();
        recyclerView.setAdapter(adapter);
        database = WydatkiBaza.getInstance(this);
        zaladuj();
    }
    private void zaladuj() {
        new Thread(() -> {
            List<Wydatki> wydatki = database.DAOWydatki().getAllExpenses();
            runOnUiThread(() -> adapter.setExpenses(wydatki));
        }).start();
    }

    protected void onResume() {
        super.onResume();
        zaladuj();
    }
}