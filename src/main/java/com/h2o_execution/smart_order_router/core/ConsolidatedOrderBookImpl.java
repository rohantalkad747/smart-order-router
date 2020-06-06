package com.h2o_execution.smart_order_router.core;

import com.h2o_execution.smart_order_router.domain.Order;
import com.h2o_execution.smart_order_router.domain.Side;
import com.h2o_execution.smart_order_router.domain.Venue;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

public class ConsolidatedOrderBookImpl implements ConsolidatedOrderBook
{

    @Override
    public void notifyUnusedLiquidity(String symbol, Side side, int amount)
    {

    }

    @Override
    public List<VenuePropertyPair<Integer>> claimLiquidity(LiquidityQuery q, Order order)
    {
        return null;
    }

    @Override
    public void addOrder(Venue venue, Order order)
    {

    }

    @Override
    public void onCancel(int id)
    {

    }

    @Override
    public void onReplace(int id, Order newOrder)
    {

    }

    @Override
    public void onReject(String clientOrderId)
    {

    }

    @Override
    public void onExecution(String clientOrderId, int shares)
    {

    }

    @EqualsAndHashCode(callSuper = true)
    @Data
    static class ClaimableOrder extends Order
    {
        private volatile int claimed;

        private synchronized boolean claim(int amount)
        {
            if (amount < 1 || amount > getLeaves())
            {
                throw new IllegalArgumentException();
            }
            claimed -= amount;
            return true;
        }
    }
}
