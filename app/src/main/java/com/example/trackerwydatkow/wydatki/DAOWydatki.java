package com.example.trackerwydatkow.wydatki;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

@Dao
public interface DAOWydatki {
    @Insert
    void insert(Wydatki wydatki);

    @Query("SELECT * FROM wydatki ORDER BY data DESC")
    List<Wydatki> getAllExpenses();

    @Query("SELECT * FROM wydatki WHERE kategoria = :category")
    List<Wydatki> getExpensesByCategory(String category);

    @Query("SELECT SUM(kwota) FROM wydatki WHERE data >= date(:startDate)")
    double getTotalExpensesAfterDate(String startDate);

    @Delete
    void delete(Wydatki wydatki);
}
