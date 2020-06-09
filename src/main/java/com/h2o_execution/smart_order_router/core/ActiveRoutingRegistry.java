package com.h2o_execution.smart_order_router.core;

import com.h2o_execution.smart_order_router.domain.Order;

import java.util.List;

public interface ActiveRoutingRegistry
{
    List<Router> getActiveRouters();

    void addActiveRouter(Router router);
}
