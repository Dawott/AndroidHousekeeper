package com.example.trackerwydatkow;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.trackerwydatkow.wydatki.Wydatki;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.List;

public class AdapterWydatki extends RecyclerView.Adapter<AdapterWydatki.ExpenseViewHolder> {
private List<Wydatki> wydatki = new ArrayList<>();

@NonNull
@Override
public ExpenseViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
    View itemView = LayoutInflater.from(parent.getContext())
            .inflate(R.layout.item_wydatek, parent, false);
    return new ExpenseViewHolder(itemView);
}

@Override
public void onBindViewHolder(@NonNull ExpenseViewHolder holder, int position) {
    Wydatki aktualnyWydatek = wydatki.get(position);
    holder.textViewName.setText(aktualnyWydatek.getNazwa());
    String currency = aktualnyWydatek.getWaluta();
    if (currency == null || currency.isEmpty()) {
        currency = "PLN";
    }
    holder.textViewAmount.setText(String.format("%.2f %s", aktualnyWydatek.getKwota(), currency));
    holder.textViewCategory.setText(aktualnyWydatek.getKategoria());
    holder.textViewDate.setText(aktualnyWydatek.getData());
}

@Override
public int getItemCount() {
    return wydatki.size();
}

public void setExpenses(List<Wydatki> wydatki) {
    this.wydatki = wydatki;
    notifyDataSetChanged();
}

static class ExpenseViewHolder extends RecyclerView.ViewHolder {
    private TextView textViewName;
    private TextView textViewAmount;
    private TextView textViewCategory;
    private TextView textViewDate;

    public ExpenseViewHolder(View itemView) {
        super(itemView);
        textViewName = itemView.findViewById(R.id.textViewName);
        textViewAmount = itemView.findViewById(R.id.textViewAmount);
        textViewCategory = itemView.findViewById(R.id.textViewCategory);
        textViewDate = itemView.findViewById(R.id.textViewDate);
    }
}
}