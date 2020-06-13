package com.h2o_execution.smart_order_router.core;

import com.h2o_execution.smart_order_router.domain.Currency;

import java.util.concurrent.ExecutionException;

public interface FXRatesService
{
    double getFXRate(Currency target, Currency against) throws ExecutionException;
}
