package com.example.trackerwydatkow.wydatki;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
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

    @ColumnInfo(name = "waluta", defaultValue = "PLN")
    private String waluta;

/*
    @ColumnInfo(name = "location")
    private String location;

    @ColumnInfo(name = "latitude")
    private double latitude;

    @ColumnInfo(name = "longitude")
    private double longitude;*/

    public Wydatki(String nazwa, double kwota, String kategoria, String data, String waluta) {
        this.nazwa = nazwa;
        this.kwota = kwota;
        this.kategoria = kategoria;
        this.data = data;
        this.waluta = waluta;
       /* this.location = location;
        this.latitude = latitude;
        this.longitude = longitude;*/
    }

    @Ignore
    public Wydatki(String nazwa, double kwota, String kategoria, String data) {
        this.nazwa = nazwa;
        this.kwota = kwota;
        this.kategoria = kategoria;
        this.data = data;
        this.waluta = "PLN";
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getNazwa() { return nazwa; }
    public double getKwota() { return kwota; }
    public String getKategoria() { return kategoria; }
    public String getData() { return data; }
    public String getWaluta() { return waluta; }

    public void setNazwa(String nazwa) { this.nazwa = nazwa; }
    public void setKwota(double kwota) { this.kwota = kwota; }
    public void setKategoria(String kategoria) { this.kategoria = kategoria; }
    public void setData(String data) { this.data = data; }
    public void setWaluta(String waluta) { this.waluta = waluta; }
}