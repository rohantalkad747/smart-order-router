package com.h2o_execution.smart_order_router.core;

import com.h2o_execution.smart_order_router.domain.Order;

public interface Router extends OrderEventsListener {
    void route(Order order);
}
