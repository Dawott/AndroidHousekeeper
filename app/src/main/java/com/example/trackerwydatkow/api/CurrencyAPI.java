package com.example.trackerwydatkow.api;

import retrofit2.Call;
import retrofit2.http.GET;

public interface CurrencyAPI {
    @GET("latest/PLN")
    Call<CurrencyResponse> getExchangeRates();
}
