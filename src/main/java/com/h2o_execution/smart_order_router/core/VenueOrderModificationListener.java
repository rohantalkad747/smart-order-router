package com.h2o_execution.smart_order_router.core;

import com.h2o_execution.smart_order_router.domain.Order;

public interface VenueOrderModificationListener extends VenueOrderListener
{
    void onCancel(int id);

    void onReplace(int id, Order newOrder);
}
