package com.h2o_execution.smart_order_router.core;

import com.h2o_execution.smart_order_router.domain.Currency;
import lombok.Data;

import java.util.HashMap;
import java.util.Map;

public class FXRatesServiceImpl implements FXRatesService
{

    @Data
    class CurrencyPair
    {
        private Currency base;
        private Currency against;
    }

    private final Map<CurrencyPair, Double> currencyTable = new HashMap<>();

    @Override
    public double getFXRate(Currency target, Currency against)
    {
        return 0;
    }

    @Override
    public void updateFXRate(Currency target, Currency against, double value)
    {

    }
}
