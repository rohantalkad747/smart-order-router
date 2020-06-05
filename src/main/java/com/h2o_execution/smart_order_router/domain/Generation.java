package com.h2o_execution.smart_order_router.domain;

import com.h2o_execution.smart_order_router.domain.Order;

public enum Generation
{
    SPRAY(0.003),
    SERIAL(0.001);

    private final double rate;

    Generation(double fees)
    {
        this.rate = fees;
    }

    public double getFees(Order o)
    {
        return rate * o.getCumulativeQuantity();
    }
}
