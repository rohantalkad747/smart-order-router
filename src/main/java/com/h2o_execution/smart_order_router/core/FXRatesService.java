package com.h2o_execution.smart_order_router.core;

import com.h2o_execution.smart_order_router.domain.Currency;

public interface FXRatesService
{
    double getFXRate(Currency target, Currency against);

    void updateFXRate(Currency target, Currency against, double value);
}
