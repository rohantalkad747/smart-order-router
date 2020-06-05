package com.h2o_execution.smart_order_router.core;

public interface VenueOrderListener
{
    void onReject(String clientOrderId);

    void onExecution(String clientOrderId, int shares);
}
