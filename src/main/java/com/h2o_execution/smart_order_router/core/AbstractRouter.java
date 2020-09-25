package com.h2o_execution.smart_order_router.core;

import com.h2o_execution.smart_order_router.domain.Order;
import com.h2o_execution.smart_order_router.domain.TimeInForce;
import com.h2o_execution.smart_order_router.domain.Venue;
import com.h2o_execution.smart_order_router.market_access.OrderManager;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.AbstractMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Data
@Slf4j
@AllArgsConstructor
public abstract class AbstractRouter implements Router {
    protected final OrderManager orderManager;
    private final OrderIdService orderIdService;
    private final ProbabilisticExecutionVenueProvider probabilisticExecutionVenueProvider;
    private final ConsolidatedOrderBook consolidatedOrderBook;
    private final RoutingConfig routingConfig;
    private final AtomicInteger totalRouted;
    private final Map<String, Order> idOrderMap;
    protected Map<Venue, Integer> routes;

    public int getTotalRouted() {
        return totalRouted.get();
    }

    public AbstractRouter(OrderIdService orderIdService, OrderManager orderManager, ProbabilisticExecutionVenueProvider probabilisticExecutionVenueProvider, ConsolidatedOrderBook consolidatedOrderBook, RoutingConfig routingConfig) {
        this.orderIdService = orderIdService;
        this.orderManager = orderManager;
        this.probabilisticExecutionVenueProvider = probabilisticExecutionVenueProvider;
        this.consolidatedOrderBook = consolidatedOrderBook;
        this.routingConfig = routingConfig;
        this.idOrderMap = new ConcurrentHashMap<>();
        this.totalRouted = new AtomicInteger();
        this.routes = new HashMap<>();
    }

    protected abstract void onDoneCreatingChildOrders();

    private void logRouteState(Order order) {
        log.debug(String.format("Routed %d/%d shares", totalRouted.get(), order.getQuantity()));
        log.debug("Routing table: \n " + routes.entrySet()
                .stream()
                .map(kv -> new AbstractMap.SimpleEntry(kv.getKey().getName(), kv.getValue()))
                .collect(Collectors.toList()));
    }

    protected abstract void onNewChildOrder(VenuePropertyPair<Order> order);

    public void sliceIntoChildren(Order order, TimeInForce timeInForce) {
        for (Entry<Venue, Integer> target : routes.entrySet()) {
            int targetLeaves = target.getValue();
            int remaining = order.getLeaves() - totalRouted.get();
            int childQuantity = Math.min(targetLeaves, remaining);
            String id = orderIdService.generateId();
            Order child = createChild(order, target.getKey(), childQuantity, timeInForce, id);
            idOrderMap.put(child.getClientOrderId(), child);
            onNewChildOrder(new VenuePropertyPair<>(child, target.getKey()));
            totalRouted.addAndGet(childQuantity);
        }
    }

    public Order createChild(Order prnt, Venue venue, int childQuantity, TimeInForce timeInForce, String newId) {
        return Order
                .builder()
                .symbol(prnt.getSymbol())
                .currency(venue.getCurrency())
                .venue(venue)
                .clientOrderId(newId)
                .side(prnt.getSide())
                .quantity(childQuantity)
                .cumulativeQuantity(0)
                .limitPrice(prnt.getLimitPrice())
                .timeInForce(timeInForce)
                .orderType(prnt.getOrderType())
                .build();
    }

    @Override
    public void route(Order order) {
        if (order.isTerminal()) {
            throw new RuntimeException("Cannot route a terminal order!");
        }
        createSweepChildOrders(order, routingConfig);
        routeStageComplete(order);
        if (orderStillMarketable(order)) {
            log.debug("Starting to post orders to venues ...");
            createPostChildOrders(order, routingConfig);
            routeStageComplete(order);
        }
        log.debug("Done routing all shares!");
    }

    private boolean orderStillMarketable(Order order) {
        return totalRouted.get() != order.getQuantity();
    }

    private void routeStageComplete(Order order) {
        logRouteState(order);
        onDoneCreatingChildOrders();
    }

    private void createPostChildOrders(Order order, RoutingConfig routingConfig) {
        List<VenuePropertyPair<Double>> venueExecutionProbabilityPairs = probabilisticExecutionVenueProvider.getVenueExecutionProbabilityPairs(order.getSymbol(), RoutingStage.POST, routingConfig);
        routes = new HashMap<>();
        for (VenuePropertyPair<Double> venueExecutionPair : venueExecutionProbabilityPairs) {
            double executionProbability = venueExecutionPair.getVal();
            int childQuantity = (int) Math.round(order.getLeaves() * executionProbability);
            routes.put(venueExecutionPair.getVenue(), childQuantity);
        }
        sliceIntoChildren(order, TimeInForce.DAY);
    }

    private void createSweepChildOrders(Order order, RoutingConfig routingConfig) {
        ConsolidatedOrderBookImpl.LiquidityQuery query = ConsolidatedOrderBookImpl.LiquidityQuery.builder()
                .type(routingConfig.getSweepType())
                .countries(routingConfig.getCountrySet())
                .limitPx(new BigDecimal(order.getLimitPrice()).setScale(2, RoundingMode.HALF_UP))
                .symbol(order.getSymbol())
                .baseCurrency(routingConfig.getBaseCurrency())
                .currencies(routingConfig.getAvailableCapital().keySet())
                .quantity(order.getLeaves())
                .side(order.getSide())
                .build();
        routes = consolidatedOrderBook.claimLiquidity(query);
        sliceIntoChildren(order, TimeInForce.IOC);
    }

    @Override
    public void onReject(String clientOrderId) {
        Order order = idOrderMap.get(clientOrderId);
        log.warn("Rejection on order", order);
        totalRouted.addAndGet(-(order.getQuantity()));
        route(order);
    }

    @Override
    public void onExecution(String clientOrderId, int shares) {
        Order order = idOrderMap.get(clientOrderId);
        order.updateCumulativeQuantity(shares);
        if (eligibleForRerouting(order)) {
            totalRouted.addAndGet(-(order.getLeaves()));
            route(order);
        }
    }

    private boolean eligibleForRerouting(Order order) {
        return order.getTimeInForce() == TimeInForce.IOC && !order.isTerminal();
    }
}
