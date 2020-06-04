package com.h2o_execution.smart_order_router.core;

public interface OrderAware
{

    void onReject(int id);

    void onExecution(int id, int shares);
}
