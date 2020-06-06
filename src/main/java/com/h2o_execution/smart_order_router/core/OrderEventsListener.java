package com.h2o_execution.smart_order_router.core;

public interface OrderEventsListener
{
    void onReject(String clientOrderId);

    void onExecution(String clientOrderId, int shares);
}
