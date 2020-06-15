package com.h2o_execution.smart_order_router.core;

import org.checkerframework.checker.units.qual.A;

import java.util.concurrent.atomic.AtomicInteger;

public class OrderIdService
{
    private AtomicInteger atomicInteger = new AtomicInteger();

    public String generateId()
    {
        return "O:" + atomicInteger.getAndIncrement();
    }
}
