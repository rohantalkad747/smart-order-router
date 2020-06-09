package com.h2o_execution.smart_order_router.core;

import com.h2o_execution.smart_order_router.domain.Order;
import com.h2o_execution.smart_order_router.domain.Venue;

import java.util.Map;

public interface ConsolidatedOrderBook extends OrderModificationEventsListener
{
    /**
     * @return a list of shares per venue that must are claimed by the router. If a SOR
     * does not use the claimed liquidity they should notify the COB by calling the {{@link #notifyUnusedLiquidity}
     * method to open it to other SOR instances (if it's still available, of course).
     */
    Map<Venue, Integer> claimLiquidity(ConsolidatedOrderBookImpl.LiquidityQuery q);

    /**
     * Adds an order to this COB.
     */
    void addOrder(Venue venue, Order order);
}
