package com.h2o_execution.smart_order_router.domain;

public enum TimeInForce
{
    DAY('0'),
    IOC('3');

    private char data;

    TimeInForce(char data)
    {
        this.data = data;
    }

    public char getValue()
    {
        return data;
    }
}
