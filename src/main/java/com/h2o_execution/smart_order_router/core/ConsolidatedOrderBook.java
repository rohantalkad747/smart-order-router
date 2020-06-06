package com.h2o_execution.smart_order_router.core;

import com.h2o_execution.smart_order_router.domain.Order;
import com.h2o_execution.smart_order_router.domain.Side;
import com.h2o_execution.smart_order_router.domain.Venue;

import java.util.List;

public interface ConsolidatedOrderBook extends OrderModificationEventsListener
{
    /**
     * Tells the COB that a SOR has outstanding claimed shares that it does not need anymore.
     * The COB will attempt to find and unclaim any claimed unfilled entries.
     */
    void notifyUnusedLiquidity(String symbol, Side side, int amount);

    /**
     * @return a list of shares per venue that must are claimed by the router. If a SOR
     * does not use the claimed liquidity they should notify the COB by calling the {{@link #notifyUnusedLiquidity}
     * method to open it to other SOR instances (if it's still available, of course).
     */
    List<VenuePropertyPair<Integer>> claimLiquidity(LiquidityQuery q, Order order);

    /**
     * Adds an order to this COB.
     */
    void addOrder(Venue venue, Order order);
}
