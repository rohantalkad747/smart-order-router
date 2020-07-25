package com.h2o_execution.smart_order_router.core;

import com.google.common.collect.Maps;
import com.h2o_execution.smart_order_router.domain.Currency;
import com.h2o_execution.smart_order_router.domain.Order;
import com.h2o_execution.smart_order_router.domain.Side;
import com.h2o_execution.smart_order_router.domain.Venue;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@ToString
@Service
public class ConsolidatedOrderBookImpl implements ConsolidatedOrderBook {
    private static final int BUY = 0;
    private static final int SELL = 1;
    private final Map<String, Map<Currency, List<Map<Double, Map<Venue, VolumeClaimPair>>>>> orderBookMap;
    private final Map<String, Order> orderMap;
    private final FXRatesService fxRatesService;

    public ConsolidatedOrderBookImpl(FXRatesService fxRatesService) {
        this.fxRatesService = fxRatesService;
        this.orderBookMap = Maps.newConcurrentMap();
        this.orderMap = Maps.newHashMap();
    }

    private Map<Double, Map<Venue, VolumeClaimPair>> getOrderBook(String symbol, Currency currency, Side clientSide) {
        return (Map<Double, Map<Venue, VolumeClaimPair>>) orderBookMap
                .computeIfAbsent(symbol, k -> Maps.newEnumMap(Currency.class))
                .computeIfAbsent(currency, k -> Arrays.asList(Maps.newHashMap(), Maps.newHashMap()))
                .get(sideIndex(clientSide));
    }

    private Map<Venue, VolumeClaimPair> getVenueClaimPairMap(String symbol, Side clientSide, double price, Currency currency) {
        Map<Double, Map<Venue, VolumeClaimPair>> ob = getOrderBook(symbol, currency, clientSide);
        return ob.computeIfAbsent(price, k -> Maps.newHashMap());
    }

    @Override
    public Map<Venue, Integer> claimLiquidity(LiquidityQuery q) {
        Map<Venue, Integer> allotted = Maps.newHashMap();
        List<Entry<Double, Map<Venue, VolumeClaimPair>>> withinLimit = getCompatiblePricePoints(q);
        int orderQuantity = q.getQuantity(), currentClaimed = 0;
        for (Entry<Double, Map<Venue, VolumeClaimPair>> pricePoint : withinLimit) {
            VvpAlloc vvpAlloc = VvpAlloc.of(allotted, orderQuantity, currentClaimed, pricePoint);
            if (vvpAlloc.isAlloc())
                return allotted;
            currentClaimed = vvpAlloc.getCurrentClaimed();
        }
        throw new RuntimeException("No allotted liquidity!");
    }

    private List<Entry<Double, Map<Venue, VolumeClaimPair>>> getCompatiblePricePoints(LiquidityQuery q) {
        return orderBookMap
                .get(q.getSymbol())
                .entrySet()
                .stream()
                .filter(x -> q.getCurrencies().contains(x.getKey()))
                .flatMap(x -> toFXAdjustedPp(q, x))
                .filter(Objects::nonNull)
                .filter(x -> q.side == Side.BUY ? x.getKey() <= q.getLimitPx() : x.getKey() >= q.getLimitPx())
                .collect(Collectors.toList());
    }

    private Stream<? extends AbstractMap.SimpleEntry<Double, Map<Venue, VolumeClaimPair>>> toFXAdjustedPp(LiquidityQuery q, Entry<Currency, List<Map<Double, Map<Venue, VolumeClaimPair>>>> x) {
        try {
            Double fxRate = fxRatesService.getFXRate(x.getKey(), q.getBaseCurrency());
            return
                    x
                            .getValue()
                            .get(sideIndex(q.getSide()))
                            .entrySet()
                            .stream()
                            .map(venueVolumeClaimPairMap -> new AbstractMap.SimpleEntry<>(venueVolumeClaimPairMap.getKey() * fxRate, venueVolumeClaimPairMap.getValue()));
        } catch (ExecutionException e) {
            return null;
        }
    }

    @Override
    public void addOrder(Order order) {
        Map<Venue, VolumeClaimPair> venueClaimPairMap = getVenueClaimPairMap(order.getSymbol(), order.getSide(), order.getLimitPrice(), order.getVenue().getCurrency());
        venueClaimPairMap.computeIfAbsent(order.getVenue(), k -> new VolumeClaimPair()).incVol(order.getLeaves());
        orderMap.put(order.getClientOrderId(), order);
    }

    @Override
    public void onExecution(String clientOrderId, int shares) {
        Order old = orderMap.get(clientOrderId);
        synchronized (old) {
            old.updateCumulativeQuantity(shares);
            incVol(old, -(shares));
            if (old.isTerminal()) {
                orderMap.remove(clientOrderId);
            }
        }
    }

    @Override
    public void onCancel(String id) {
        Order order = orderMap.get(id);
        incVol(order, -(order.getLeaves()));
        orderMap.remove(id);
    }

    @Override
    public void onReplace(String id, Order newOrder) {
        orderMap.put(newOrder.getClientOrderId(), newOrder);
        Order old = orderMap.get(id);
        orderMap.remove(id);
        int diff = old.getLeaves() - newOrder.getLeaves();
        incVol(old, diff);
    }

    private void incVol(Order old, int diff) {
        orderBookMap
                .computeIfAbsent(old.getSymbol(), k -> new EnumMap<>(Currency.class))
                .computeIfAbsent(old.getCurrency(), k -> new ArrayList<>())
                .get(sideIndex(old.getSide()))
                .computeIfAbsent(old.getLimitPrice(), k -> new HashMap<>())
                .get(old.getVenue())
                .incVol(diff);
    }

    private int sideIndex(Side side) {
        return side == Side.BUY ? SELL : BUY;
    }

    @Override
    public void onReject(String clientOrderId) {
        // Don't care about rejections since we never send orders from here
    }

    @Data
    @Builder
    @AllArgsConstructor
    public static class LiquidityQuery {
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
    private static class VolumeClaimPair {
        private volatile int totalVolume = 0;
        private volatile int claimed = 0;

        /**
         * Attempts the claim the given {@code amount}.
         *
         * @return the number of shares actually claimed.
         */
        public synchronized int claim(int amount) {
            int amtLeft = totalVolume - claimed;
            int actuallyClaimed = Math.min(amtLeft, amount);
            claimed -= actuallyClaimed;
            return actuallyClaimed;
        }

        public synchronized void incVol(int amount) {
            totalVolume += amount;
        }
    }

    private static class VvpAlloc {
        private boolean isAlloc;
        private Map<Venue, Integer> allotted;
        private int orderQuantity;
        private int currentClaimed;
        private Entry<Double, Map<Venue, VolumeClaimPair>> pricePoint;

        public VvpAlloc(Map<Venue, Integer> allotted, int orderQuantity, int currentClaimed, Entry<Double, Map<Venue, VolumeClaimPair>> pricePoint) {
            this.allotted = allotted;
            this.orderQuantity = orderQuantity;
            this.currentClaimed = currentClaimed;
            this.pricePoint = pricePoint;
        }

        public static VvpAlloc of(Map<Venue, Integer> allotted, int orderQuantity, int currentClaimed, Entry<Double, Map<Venue, VolumeClaimPair>> pricePoint) {
            VvpAlloc vvpAlloc = new VvpAlloc(allotted, orderQuantity, currentClaimed, pricePoint);
            vvpAlloc.alloc();
            return vvpAlloc;
        }

        boolean isAlloc() {
            return isAlloc;
        }

        public int getCurrentClaimed() {
            return currentClaimed;
        }

        public VvpAlloc alloc() {
            for (Entry<Venue, VolumeClaimPair> volumeVenueClaimPair : pricePoint.getValue().entrySet()) {
                Venue venue = volumeVenueClaimPair.getKey();
                if (venue.isAvailable()) {
                    tryAlloc(volumeVenueClaimPair);
                    if (currentClaimed == orderQuantity) {
                        isAlloc = true;
                        return this;
                    }
                }
            }
            isAlloc = false;
            return this;
        }

        private void tryAlloc(Entry<Venue, VolumeClaimPair> volumeVenueClaimPair) {
            int leftToClaim = orderQuantity - currentClaimed;
            VolumeClaimPair vcp = volumeVenueClaimPair.getValue();
            final int finalCurrentClaimed = vcp.claim(leftToClaim);
            currentClaimed += finalCurrentClaimed;
            allotted.merge(volumeVenueClaimPair.getKey(), 0, (k, v) -> finalCurrentClaimed + v);
        }
    }
}
