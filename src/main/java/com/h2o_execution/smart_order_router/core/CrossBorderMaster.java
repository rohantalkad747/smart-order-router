package com.h2o_execution.smart_order_router.core;

public interface CrossBorderMaster
{
    boolean isCrossBorderAvailable(String symbol);

    void addSymbol(String symbol);

    void removeSymbol(String symbol);
}
