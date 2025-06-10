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

    @ColumnInfo(name = "receipt_image_url")
    private String receiptImageUrl;

    @ColumnInfo(name = "is_from_receipt", defaultValue = "0")
    private boolean isFromReceipt;

    @ColumnInfo(name = "ocr_confidence", defaultValue = "0")
    private float ocrConfidence;

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
        this.isFromReceipt = false;
        this.ocrConfidence = 0.0f;
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
        this.isFromReceipt = false;
        this.ocrConfidence = 0.0f;
    }

    @Ignore
    public Wydatki(String nazwa, double kwota, String kategoria, String data, String waluta,
                   String receiptImageUrl, boolean isFromReceipt, float ocrConfidence) {
        this.nazwa = nazwa;
        this.kwota = kwota;
        this.kategoria = kategoria;
        this.data = data;
        this.waluta = waluta;
        this.receiptImageUrl = receiptImageUrl;
        this.isFromReceipt = isFromReceipt;
        this.ocrConfidence = ocrConfidence;
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

    public String getReceiptImageUrl() { return receiptImageUrl; }
    public void setReceiptImageUrl(String receiptImageUrl) { this.receiptImageUrl = receiptImageUrl; }

    public boolean isFromReceipt() { return isFromReceipt; }
    public void setFromReceipt(boolean fromReceipt) { isFromReceipt = fromReceipt; }

    public float getOcrConfidence() { return ocrConfidence; }
    public void setOcrConfidence(float ocrConfidence) { this.ocrConfidence = ocrConfidence; }
}