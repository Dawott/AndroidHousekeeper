package com.example.trackerwydatkow.wydatki;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "wydatki")
public class Wydatki {
    @PrimaryKey(autoGenerate = true)
    private int id;

    @ColumnInfo(name = "nazwa")
    private String nazwa;

    @ColumnInfo(name = "kwota")
    private double kwota;

    @ColumnInfo(name = "kategoria")
    private String kategoria;

    @ColumnInfo(name = "data")
    private String data;

    public Wydatki(String nazwa, double kwota, String kategoria, String data) {
        this.nazwa = nazwa;
        this.kwota = kwota;
        this.kategoria = kategoria;
        this.data = data;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getNazwa() { return nazwa; }
    public double getKwota() { return kwota; }
    public String getKategoria() { return kategoria; }
    public String getData() { return data; }
}