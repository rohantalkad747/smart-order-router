package com.h2o_execution.smart_order_router.core;

import com.h2o_execution.smart_order_router.domain.Order;
import com.h2o_execution.smart_order_router.domain.Venue;

import java.util.Map;

public interface ConsolidatedOrderBook extends OrderModificationEventsListener {

    Map<Venue, Integer> claimLiquidity(ConsolidatedOrderBookImpl.LiquidityQuery q);

    void addOrder(Order order);
}
