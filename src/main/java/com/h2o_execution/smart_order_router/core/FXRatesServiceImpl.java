package com.h2o_execution.smart_order_router.core;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.h2o_execution.smart_order_router.domain.Currency;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class FXRatesServiceImpl implements FXRatesService
{
    private static final int CURRENCY_EXPIRY_TIME_SECONDS = 10;
    private final RestTemplate restTemplate;
    private final Map<Currency, LoadingCache<Currency, Double>> currencyTable;

    public FXRatesServiceImpl(RestTemplate restTemplate)
    {
        this.restTemplate = restTemplate;
        this.currencyTable = new EnumMap<>(Currency.class);
        initCurrencies();
    }

    private void initCurrencies()
    {
        Currency[] currencies = Currency.values();
        for (Currency currency : currencies)
        {
            LoadingCache<Currency, Double> loadingCache = CacheBuilder
                    .newBuilder()
                    .maximumSize(currencies.length)
                    .expireAfterAccess(CURRENCY_EXPIRY_TIME_SECONDS, TimeUnit.SECONDS)
                    .build(new CacheLoader<>()
                    {
                        public Double load(Currency against)
                        {
                            return getRealTimeFXRate(currency, against);
                        }
                    });
            currencyTable.put(currency, loadingCache);
        }
    }

    @Override
    public double getFXRate(Currency target, Currency against) throws ExecutionException
    {
        if (target == against)
        {
            return 1.0;
        }
        return currencyTable.get(target).get(against);
    }


    public double getRealTimeFXRate(Currency target, Currency against)
    {
        String url = "https://api.exchangeratesapi.io/latest?base=" + target.toString() + "&symbols=" + against.toString();
        JsonNode forObject = restTemplate.getForObject(url, JsonNode.class);
        return forObject.get("rates").get(against.toString()).asDouble();
    }
}
