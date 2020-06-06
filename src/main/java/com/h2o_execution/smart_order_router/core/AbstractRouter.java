package com.h2o_execution.smart_order_router.core;

import com.h2o_execution.smart_order_router.domain.Order;
import com.h2o_execution.smart_order_router.domain.TimeInForce;
import com.h2o_execution.smart_order_router.domain.Venue;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@AllArgsConstructor
public abstract class AbstractRouter implements Router
{
    private final OrderIdService orderIdService;
    private final ProbabilisticExecutionVenueProvider probabilisticExecutionVenueProvider;
    private final ConsolidatedOrderBook consolidatedOrderBook;
    private final RoutingConfig routingConfig;
    private final AtomicInteger totalRouted;
    private final Map<String, Order> idOrderMap;
    protected List<VenuePropertyPair<Integer>> currentVenueTargets;

    public AbstractRouter(OrderIdService orderIdService, ProbabilisticExecutionVenueProvider probabilisticExecutionVenueProvider, ConsolidatedOrderBook consolidatedOrderBook, RoutingConfig routingConfig)
    {
        this.orderIdService = orderIdService;
        this.probabilisticExecutionVenueProvider = probabilisticExecutionVenueProvider;
        this.consolidatedOrderBook = consolidatedOrderBook;
        this.routingConfig = routingConfig;
        this.idOrderMap = new ConcurrentHashMap<>();
        this.totalRouted = new AtomicInteger();
    }

    public void sendToFixOut(VenuePropertyPair<Order> venuePropertyPair)
    {
    }

    protected abstract void onDoneCreatingChildOrders();

    protected abstract void onNewChildOrder(VenuePropertyPair<Order> venuePropertyPair);

    public void sliceIntoChildren(Order order, TimeInForce timeInForce)
    {
        Iterator<VenuePropertyPair<Integer>> venueChildOrderIterator = currentVenueTargets.iterator();
        for (VenuePropertyPair<Integer> venueOrderPair = venueChildOrderIterator.next();
             venueChildOrderIterator.hasNext() && totalRouted.get() <= order.getQuantity();
             venueOrderPair = venueChildOrderIterator.next())
        {
            int targetLeaves = venueOrderPair.getVal();
            int remaining = order.getLeaves() - totalRouted.get();
            int childQuantity = Math.min(targetLeaves, remaining);
            String id = orderIdService.generateId();
            Order child = order.createChild(childQuantity, timeInForce, id);
            idOrderMap.put(child.getClientOrderId(), child);
            Venue venue = venueOrderPair.getVenue();
            onNewChildOrder(new VenuePropertyPair<>(child, venue));
            totalRouted.addAndGet(childQuantity);
        }
    }

    @Override
    public void route(Order order)
    {
        if (order.isTerminal())
        {
            throw new RuntimeException("Cannot route a terminal order!");
        }
        createSweepChildOrders(order, routingConfig);
        onDoneCreatingChildOrders();
        if (orderStillMarketable(order))
        {
            createPostChildOrders(order, routingConfig);
            onDoneCreatingChildOrders();
        }
    }

    private void createPostChildOrders(Order order, RoutingConfig routingConfig)
    {
        List<VenuePropertyPair<Double>> venueExecutionProbabilityPairs = probabilisticExecutionVenueProvider.getVenueExecutionProbabilityPairs(order.getSymbol(), RoutingStage.POST, routingConfig);
        currentVenueTargets = new ArrayList<>();
        for (VenuePropertyPair<Double> venueExecutionPair : venueExecutionProbabilityPairs)
        {
            double executionProbability = venueExecutionPair.getVal();
            int childQuantity = (int) (order.getLeaves() * executionProbability);
            currentVenueTargets.add(new VenuePropertyPair<>(childQuantity, venueExecutionPair.getVenue()));
        }
        sliceIntoChildren(order, TimeInForce.DAY);
    }

    private void createSweepChildOrders(Order order, RoutingConfig routingConfig)
    {
        LiquidityQuery query = new LiquidityQuery(routingConfig.getSweepType(), routingConfig.getRoutingCountry());
        currentVenueTargets = consolidatedOrderBook.claimLiquidity(query, order);
        sliceIntoChildren(order, TimeInForce.IOC);
    }

    private boolean orderStillMarketable(Order order)
    {
        return totalRouted.get() != order.getQuantity();
    }

    @Override
    public void onReject(String clientOrderId)
    {
        Order order = idOrderMap.get(clientOrderId);
        log.warn("Rejection on order", order);
        totalRouted.addAndGet(-order.getQuantity());
        route(order);
    }

    @Override
    public void onExecution(String clientOrderId, int shares)
    {
        Order order = idOrderMap.get(clientOrderId);
        order.updateCumulativeQuantity(shares);
        if (eligibleForRerouting(order))
        {
            totalRouted.addAndGet(-order.getLeaves());
            route(order);
        }
    }

    private boolean eligibleForRerouting(Order order)
    {
        return order.getTimeInForce() == TimeInForce.IOC && !order.isTerminal();
    }
}
