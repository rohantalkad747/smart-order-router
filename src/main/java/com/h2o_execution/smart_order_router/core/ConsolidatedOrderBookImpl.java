package com.h2o_execution.smart_order_router.core;

import com.h2o_execution.smart_order_router.domain.Order;
import com.h2o_execution.smart_order_router.domain.Side;
import com.h2o_execution.smart_order_router.domain.Venue;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class ConsolidatedOrderBookImpl implements ConsolidatedOrderBook
{
    private static final int BUY = 0;
    private static final int SELL = 1;
    // Maps symbols to their buy and sell side COBs. Each COB has a mapping of price points to a list of volume venue claim pairs.
    private Map<String, Map<Double, Map<Venue, VolumeClaimPair>>[]> cob;
    private Map<String, Order> orderMap;

    @Data
    @Builder
    @AllArgsConstructor
    public static class LiquidityQuery
    {
        private Venue.Type type;
        private String symbol;
        private int quantity;
        private double limitPx;
        private Side side;
        private Country country; // Optional parameter
    }

    @Data
    private static class VolumeClaimPair
    {
        private volatile int totalVolume;
        private volatile int claimed;

        /**
         * Attempts the claim the given {@code amount}.
         * @return the number of shares actually claimed.
         */
        public synchronized int claim(int amount)
        {
            int amtLeft = totalVolume - claimed;
            int actuallyClaimed = Math.min(amtLeft, amount);
            claimed -= actuallyClaimed;
            return actuallyClaimed;
        }

        public synchronized void incVol(int amount)
        {
            totalVolume += amount;
        }
    }

    public ConsolidatedOrderBookImpl()
    {
        this.cob = new ConcurrentHashMap<>();
        this.orderMap = new HashMap<>();
    }

    private Map<Double, Map<Venue, VolumeClaimPair>> getOrderBook(String symbol, Side clientSide)
    {
        return cob.get(symbol)[clientSide == Side.BUY ? SELL : BUY];
    }

    private Map<Venue, VolumeClaimPair> getVenueClaimPairMap(String symbol, Side clientSide, double price)
    {
        Map<Double, Map<Venue, VolumeClaimPair>> ob = getOrderBook(symbol, clientSide);
        return ob.get(price);
    }

    @Override
    public Map<Venue, Integer> claimLiquidity(LiquidityQuery q)
    {
        Map<Venue, Integer> allotedLiquiditiy = new HashMap<>();
        Set<Entry<Double, Map<Venue, VolumeClaimPair>>> withinLimit = getCompatiblePricePoints(q);
        int orderQuantity = q.getQuantity();
        int currentClaimed = 0;
        Iterator<Entry<Double, Map<Venue, VolumeClaimPair>>> ppIter = withinLimit.iterator();
        for
        (
                Entry<Double, Map<Venue, VolumeClaimPair>> pricePoint = ppIter.next();
                ppIter.hasNext();
                pricePoint = ppIter.next()
        )
        {
            Iterator<Entry<Venue, VolumeClaimPair>> vvcpIter = pricePoint.getValue().entrySet().iterator();
            for
            (
                    Entry<Venue, VolumeClaimPair> vvcp = vvcpIter.next();
                    vvcpIter.hasNext();
                    vvcp = vvcpIter.next())
            {
                int leftToClaim = orderQuantity - currentClaimed;
                VolumeClaimPair vcp = vvcp.getValue();
                final int finalCurrentClaimed = currentClaimed + vcp.claim(leftToClaim); // 'final' needed for lambda merge
                currentClaimed += finalCurrentClaimed;
                allotedLiquiditiy.merge(vvcp.getKey(), 0, (k, v) ->  finalCurrentClaimed + v);
                if ( currentClaimed == orderQuantity )
                {
                    return allotedLiquiditiy;
                }
            }
        }
        throw new RuntimeException("No allotted liquidity!");
    }

    private Set<Entry<Double, Map<Venue, VolumeClaimPair>>> getCompatiblePricePoints(LiquidityQuery q)
    {
        Map<Double, Map<Venue, VolumeClaimPair>> orderBook = getOrderBook(q.getSymbol(), q.getSide());
        return orderBook
                .entrySet()
                .stream()
                .filter(pricePoint -> pricePoint.getKey() < q.getLimitPx())
                .collect(Collectors.toSet());
    }

    @Override
    public void addOrder(Venue venue, Order order)
    {
        getVenueClaimPairMap(order.getSymbol(), order.getSide(), order.getLimitPrice());
    }

    @Override
    public void onExecution(String clientOrderId, int shares)
    {
        Order old = orderMap.get(clientOrderId);
        orderMap.remove(clientOrderId);
        incVol(old, -(shares));
    }

    @Override
    public void onCancel(String id)
    {
        Order order = orderMap.get(id);
        incVol(order, -(order.getLeaves()));
        order.setQuantity(0);
    }

    @Override
    public void onReplace(String id, Order newOrder)
    {
        orderMap.put(newOrder.getClientOrderId(), newOrder);
        Order old = orderMap.get(id);
        orderMap.remove(id);
        int diff = old.getLeaves() - newOrder.getLeaves();
        incVol(old, diff);
    }

    private void incVol(Order old, int diff)
    {
        cob
                .get(old.getSymbol())[old.getSide() == Side.BUY ? SELL : BUY]
                .get(old.getLimitPrice())
                .get(old.getVenue())
                .incVol(diff);
    }

    @Override
    public void onReject(String clientOrderId)
    {
        // Don't care about rejections since we never send orders from here
    }
}
