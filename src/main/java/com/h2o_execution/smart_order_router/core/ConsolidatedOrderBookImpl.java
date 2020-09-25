package com.h2o_execution.smart_order_router.core;

import com.google.common.collect.Lists;
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
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;

import java.io.StreamTokenizer;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.ExecutionException;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@ToString
@Service
public class ConsolidatedOrderBookImpl implements ConsolidatedOrderBook {
    private static final int BUY = 0;
    private static final int SELL = 1;
    private final Map<String, Map<Currency, List<Map<BigDecimal, Map<Venue, VolumeClaimPair>>>>> orderBookMap;
    private final Map<String, Order> orderMap;
    private final FXRatesService fxRatesService;

    public ConsolidatedOrderBookImpl(FXRatesService fxRatesService) {
        this.fxRatesService = fxRatesService;
        this.orderBookMap = Maps.newConcurrentMap();
        this.orderMap = Maps.newHashMap();
    }

    private Map<BigDecimal, Map<Venue, VolumeClaimPair>> getOrderBook(String symbol, Currency currency, Side clientSide) {
        return orderBookMap
                .computeIfAbsent(symbol, k -> Maps.newEnumMap(Currency.class))
                .computeIfAbsent(currency, k -> Arrays.asList(Maps.newHashMap(), Maps.newHashMap()))
                .get(clientSide.ordinal());
    }

    private Map<Venue, VolumeClaimPair> getVenueClaimPairMap(String symbol, Side clientSide, BigDecimal price, Currency currency) {
        Map<BigDecimal, Map<Venue, VolumeClaimPair>> ob = getOrderBook(symbol, currency, clientSide);
        return ob.computeIfAbsent(price, k -> Maps.newHashMap());

    }

    @Override
    public Map<Venue, Integer> claimLiquidity(LiquidityQuery q) {
        Map<Venue, Integer> allotted = Maps.newHashMap();
        List<Entry<BigDecimal, Map<Venue, VolumeClaimPair>>> withinLimit = getCompatiblePricePoints(q);
        if (withinLimit != null) {
            int orderQuantity = q.getQuantity(), currentClaimed = 0;
            for (Entry<BigDecimal, Map<Venue, VolumeClaimPair>> pricePoint : withinLimit) {
                VvpAlloc vvpAlloc = VvpAlloc.of(allotted, orderQuantity, currentClaimed, pricePoint);
                if (vvpAlloc.isAlloc()) {
                    return allotted;
                }
                currentClaimed = vvpAlloc.getCurrentClaimed();
            }
        }
        return allotted;
    }

    private List<Entry<BigDecimal, Map<Venue, VolumeClaimPair>>> getCompatiblePricePoints(LiquidityQuery q) {
        Map<Currency, List<Map<BigDecimal, Map<Venue, VolumeClaimPair>>>> currencyListMap = orderBookMap.get(q.getSymbol());
        if (currencyListMap == null) {
            return null;
        }
        return currencyListMap
                .entrySet()
                .stream()
                .filter(isCompatibleCurrency(q))
                .flatMap(toFxAdjustedPp(q))
                .filter(Objects::nonNull)
                .filter(tradableSide(q))
                .sorted(Comparator.comparing(entry -> entry.getKey().doubleValue()))
                .collect(Collectors.toList());
    }

    @NotNull
    private Predicate<Entry<BigDecimal, Map<Venue, VolumeClaimPair>>> tradableSide(LiquidityQuery q) {
        return x -> (q.side == Side.BUY) ?
                x.getKey().doubleValue() <= q.getLimitPx().doubleValue()
                :
                x.getKey().doubleValue() >= q.getLimitPx().doubleValue();
    }

    @NotNull
    private Function<Entry<Currency, List<Map<BigDecimal, Map<Venue, VolumeClaimPair>>>>, Stream<? extends Entry<BigDecimal, Map<Venue, VolumeClaimPair>>>> toFxAdjustedPp(LiquidityQuery q) {
        return x -> toFXAdjustedPp(q, x);
    }

    @NotNull
    private Predicate<Entry<Currency, List<Map<BigDecimal, Map<Venue, VolumeClaimPair>>>>> isCompatibleCurrency(LiquidityQuery q) {
        return x -> q.getCurrencies().contains(x.getKey());
    }

    private Stream<Entry<BigDecimal, Map<Venue, VolumeClaimPair>>> toFXAdjustedPp(LiquidityQuery q, Entry<Currency, List<Map<BigDecimal, Map<Venue, VolumeClaimPair>>>> x) {
        try {
            Stream<Entry<BigDecimal, Map<Venue, VolumeClaimPair>>> ppStream = getRawPpStream(q, x);
            if (x.getKey() == q.getBaseCurrency()) {
                return ppStream;
            }
            return convertPpStreamCurrency(q, x, ppStream);
        } catch (ExecutionException e) {
            return null;
        }
    }

    private Stream<Entry<BigDecimal, Map<Venue, VolumeClaimPair>>> convertPpStreamCurrency(LiquidityQuery q, Entry<Currency, List<Map<BigDecimal, Map<Venue, VolumeClaimPair>>>> x, Stream<Entry<BigDecimal, Map<Venue, VolumeClaimPair>>> ppStream) throws ExecutionException {
        double fxRate = fxRatesService.getFXRate(x.getKey(), q.getBaseCurrency());
        return ppStream.map(doFxConversion(fxRate));
    }

    @NotNull
    private Function<Entry<BigDecimal, Map<Venue, VolumeClaimPair>>, AbstractMap.SimpleEntry<BigDecimal, Map<Venue, VolumeClaimPair>>> doFxConversion(double fxRate) {
        return venueVolumeClaimPairMap -> new AbstractMap.SimpleEntry<>(getLimitPxTwoDecimalPlaces(venueVolumeClaimPairMap.getKey().doubleValue() * fxRate), venueVolumeClaimPairMap.getValue());
    }

    private Stream<Entry<BigDecimal, Map<Venue, VolumeClaimPair>>> getRawPpStream(LiquidityQuery q, Entry<Currency, List<Map<BigDecimal, Map<Venue, VolumeClaimPair>>>> x) {
        return x
                .getValue()
                .get(sideIndex(q.getSide()))
                .entrySet()
                .stream();
    }

    @Override
    public void addOrder(Order order) {
        BigDecimal limitPxTwoDecimalPlaces = getLimitPxTwoDecimalPlaces(order.getLimitPrice());
        Map<Venue, VolumeClaimPair> venueClaimPairMap = getVenueClaimPairMap(order.getSymbol(), order.getSide(), limitPxTwoDecimalPlaces, order.getVenue().getCurrency());
        venueClaimPairMap.computeIfAbsent(order.getVenue(), k -> new VolumeClaimPair()).incVol(order.getLeaves());
        orderMap.put(order.getClientOrderId(), order);
    }

    private BigDecimal getLimitPxTwoDecimalPlaces(double px) {
        return new BigDecimal(px).setScale(2, RoundingMode.HALF_UP);
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
                .computeIfAbsent(old.getSymbol(), k -> Maps.newEnumMap(Currency.class))
                .computeIfAbsent(old.getCurrency(), k -> Lists.newArrayList())
                .get(sideIndex(old.getSide()))
                .computeIfAbsent(getLimitPxTwoDecimalPlaces(old.getLimitPrice()), k -> Maps.newHashMap())
                .get(old.getVenue())
                .incVol(diff);
    }

    private int sideIndex(Side side) {
        return side == Side.BUY ? SELL : BUY;
    }

    @Override
    public void onReject(String clientOrderId) {
        log.info("Received a rejection for ClorId: " + clientOrderId);
    }

    @Data
    @Builder
    @AllArgsConstructor
    public static class LiquidityQuery {
        private Venue.Type type;
        private String symbol;
        private int quantity;
        private BigDecimal limitPx;
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
        private boolean isAlloc = false;
        private Map<Venue, Integer> allotted;
        private int orderQuantity;
        private int currentClaimed;
        private Entry<BigDecimal, Map<Venue, VolumeClaimPair>> pricePoint;

        public VvpAlloc(Map<Venue, Integer> allotted, int orderQuantity, int currentClaimed, Entry<BigDecimal, Map<Venue, VolumeClaimPair>> pricePoint) {
            this.allotted = allotted;
            this.orderQuantity = orderQuantity;
            this.currentClaimed = currentClaimed;
            this.pricePoint = pricePoint;
        }

        public static VvpAlloc of(Map<Venue, Integer> allotted, int orderQuantity, int currentClaimed, Entry<BigDecimal, Map<Venue, VolumeClaimPair>> pricePoint) {
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

        public void alloc() {
            for (Entry<Venue, VolumeClaimPair> volumeVenueClaimPair : pricePoint.getValue().entrySet()) {
                Venue venue = volumeVenueClaimPair.getKey();
                if (venue.isAvailable()) {
                    tryAlloc(volumeVenueClaimPair);
                    if (currentClaimed == orderQuantity) {
                        isAlloc = true;
                        return;
                    }
                }
            }
        }

        private void tryAlloc(Entry<Venue, VolumeClaimPair> volumeVenueClaimPair) {
            int leftToClaim = orderQuantity - currentClaimed;
            VolumeClaimPair vcp = volumeVenueClaimPair.getValue();
            final int finalCurrentClaimed = vcp.claim(leftToClaim);
            currentClaimed += finalCurrentClaimed;
            allotted.merge(volumeVenueClaimPair.getKey(), finalCurrentClaimed, (k, v) -> finalCurrentClaimed + v);
        }
    }
}
