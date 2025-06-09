package com.example.trackerwydatkow.api;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;

public interface CurrencyAPI {
    @GET("latest/{baseCurrency}")
    Call<CurrencyResponse> getExchangeRates(@Path("baseCurrency") String baseCurrency);

    @GET("latest/PLN")
    Call<CurrencyResponse> getExchangeRates();
}
