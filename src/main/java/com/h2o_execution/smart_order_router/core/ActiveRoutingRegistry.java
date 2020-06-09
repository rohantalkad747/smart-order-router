package com.h2o_execution.smart_order_router.core;

import java.util.List;

public interface ActiveRoutingRegistry
{
    List<Router> getActiveRouters();

    void addActiveRouter(Router router);
}
