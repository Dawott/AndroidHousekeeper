package com.example.trackerwydatkow.api;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class CurrencyService {
    private static final String BASE_URL = "https://api.exchangerate-api.com/v4/latest/";
    private static CurrencyAPI api;

    public static CurrencyAPI getAPI() {
        if (api == null) {
            Retrofit retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
            api = retrofit.create(CurrencyAPI.class);
        }
        return api;
    }
}
