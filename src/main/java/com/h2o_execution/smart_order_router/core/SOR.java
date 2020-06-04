package com.h2o_execution.smart_order_router.core;

import com.h2o_execution.smart_order_router.domain.Order;

public interface SOR extends OrderAware
{
    void route(Order order);
}
