package com.h2o_execution.smart_order_router.market_access;

import com.h2o_execution.smart_order_router.core.Router;
import com.h2o_execution.smart_order_router.domain.Order;
import com.h2o_execution.smart_order_router.domain.Venue;

public interface IOrderManager
{
    void sendOrder(Venue v, Order order, Router router);
}
