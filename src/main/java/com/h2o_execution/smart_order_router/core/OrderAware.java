package com.h2o_execution.smart_order_router.core;

import com.h2o_execution.smart_order_router.domain.Order;

public interface OrderAware
{
    void onExecution(int id, int shares);

    void onCancel(int id);

    void onReject(int id);

    void onReplace(int id, Order newOrder);
}
