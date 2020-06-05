package com.h2o_execution.smart_order_router.core;

import com.h2o_execution.smart_order_router.domain.Order;
import com.h2o_execution.smart_order_router.domain.Venue;

import java.util.List;

public interface ConsolidatedOrderBook extends VenueOrderModificationListener
{
    List<VenuePropertyPair<Integer>> getOutstandingSharesPerVenue(LiquidityQuery q, Order order);

    void addOrder(Venue venue, Order order);
}
