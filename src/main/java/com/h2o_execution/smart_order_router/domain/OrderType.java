package com.h2o_execution.smart_order_router.domain;

public enum OrderType
{
    MARKET('1'),
    LIMIT('2');

    private final char data;

    OrderType(char data)
    {
        this.data = data;
    }

    public char getValue()
    {
        return data;
    }
}
