package com.h2o_execution.smart_order_router.core;

import com.h2o_execution.smart_order_router.domain.Order;

public interface OrderModificationEventsListener extends OrderEventsListener
{
    void onCancel(String id);

    void onReplace(String id, Order newOrder);
}
