package com.h2o_execution.smart_order_router.core;

import java.util.concurrent.atomic.AtomicInteger;

public class OrderIdService {
    private final AtomicInteger atomicInteger = new AtomicInteger();

    public String generateId() {
        return "O:" + atomicInteger.getAndIncrement();
    }
}
