package com.h2o_execution.smart_order_router.core;

import com.h2o_execution.smart_order_router.domain.Currency;
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
    private final Map<String, Map<Currency, Map<Double, Map<Venue, VolumeClaimPair>>[]>> orderBookMap;
    private final Map<String, Order> orderMap;
    private FXRatesService fxRatesService;

    public ConsolidatedOrderBookImpl()
    {
        this.orderBookMap = new ConcurrentHashMap<>();
        this.orderMap = new HashMap<>();
    }

    private Map<Double, Map<Venue, VolumeClaimPair>> getOrderBook(String symbol, Currency currency, Side clientSide)
    {
        return orderBookMap.get(symbol).get(currency)[sideIndex(clientSide)];
    }

    private Map<Venue, VolumeClaimPair> getVenueClaimPairMap(String symbol, Side clientSide, double price, Currency currency)
    {
        Map<Double, Map<Venue, VolumeClaimPair>> ob = getOrderBook(symbol, currency, clientSide);
        return ob.get(price);
    }

    @Override
    public Map<Venue, Integer> claimLiquidity(LiquidityQuery q)
    {
        Map<Venue, Integer> allotted = new HashMap<>();
        List<Entry<Double, Map<Venue, VolumeClaimPair>>> withinLimit = getCompatiblePricePoints(q);
        int orderQuantity = q.getQuantity(), currentClaimed = 0;
        for (Entry<Double, Map<Venue, VolumeClaimPair>> pricePoint : withinLimit)
        {
            for (Entry<Venue, VolumeClaimPair> volumeVenueClaimPair : pricePoint.getValue().entrySet())
            {
                Venue venue = volumeVenueClaimPair.getKey();
                if (venue.isAvailable())
                {
                    int leftToClaim = orderQuantity - currentClaimed;
                    VolumeClaimPair vcp = volumeVenueClaimPair.getValue();
                    final int finalCurrentClaimed = vcp.claim(leftToClaim);
                    currentClaimed += finalCurrentClaimed;
                    allotted.merge(volumeVenueClaimPair.getKey(), 0, (k, v) -> finalCurrentClaimed + v);
                    if (currentClaimed == orderQuantity)
                    {
                        return allotted;
                    }
                }
            }
        }
        throw new RuntimeException("No allotted liquidity!");
    }

    private List<Entry<Double, Map<Venue, VolumeClaimPair>>> getCompatiblePricePoints(LiquidityQuery q)
    {
        return orderBookMap
                .get(q.getSymbol())
                .entrySet()
                .stream()
                .filter(x -> q.getCurrencies().contains(x.getKey()))
                .flatMap(x ->
                {
                    Map<Double, Map<Venue, VolumeClaimPair>> ppToVenues = x.getValue()[sideIndex(q.getSide())];
                    double fxRate = fxRatesService.getFXRate(q.getBaseCurrency(), x.getKey());
                    return
                            ppToVenues
                                    .entrySet()
                                    .stream()
                                    .map(venueVolumeClaimPairMap -> new AbstractMap.SimpleEntry<Double, Map<Venue, VolumeClaimPair>>(venueVolumeClaimPairMap.getKey() * fxRate, venueVolumeClaimPairMap.getValue()));
                })
                .filter(x -> x.getKey() < q.getLimitPx())
                .collect(Collectors.toList());
    }

    @Override
    public void addOrder(Venue venue, Order order)
    {
        Map<Venue, VolumeClaimPair> venueClaimPairMap = getVenueClaimPairMap(order.getSymbol(), order.getSide(), order.getLimitPrice(), venue.getCurrency());
        venueClaimPairMap.get(venue).incVol(order.getLeaves());
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
        orderBookMap
                .get(old.getSymbol())
                .get(old.getCurrency())[sideIndex(old.getSide())]
                .get(old.getLimitPrice())
                .get(old.getVenue())
                .incVol(diff);
    }

    private int sideIndex(Side side)
    {
        return side == Side.BUY ? SELL : BUY;
    }

    @Override
    public void onReject(String clientOrderId)
    {
        // Don't care about rejections since we never send orders from here
    }

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
        private Currency baseCurrency;
        private Set<Currency> currencies;
        private Set<Country> countries;
    }

    @Data
    private static class VolumeClaimPair
    {
        private volatile int totalVolume;
        private volatile int claimed;

        /**
         * Attempts the claim the given {@code amount}.
         *
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
}
