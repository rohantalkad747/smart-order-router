package com.h2o_execution.smart_order_router.domain;

public enum Side {
    BUY('1'),
    SELL('2');

    private final char data;

    Side(char data) {
        this.data = data;
    }

    public char getValue() {
        return data;
    }
}
