package com.h2o_execution.smart_order_router.core;

public interface ForeignExchangeService
{
    void exchange(int clientId, double amount, Currency currency);
}
