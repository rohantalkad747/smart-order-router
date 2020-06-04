package com.h2o_execution.smart_order_router.core;

import com.h2o_execution.smart_order_router.domain.Order;

import java.util.List;

public interface ConsolidatedOrderBook extends ModOrderAware
{
    List<VenuePropertyPair<Integer>> getOutstandingSharesPerVenue(LiquidityQuery q, Order order);

    void addOrder(Venue venue, Order order);
}
