package com.h2o_execution.smart_order_router.core;

import com.h2o_execution.smart_order_router.domain.Order;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@AllArgsConstructor
public abstract class SORSupport implements SOR
{
    protected List<VenuePropertyPair<Integer>> currentVenueTargets;
    private final OrderIdService orderIdService;
    private final ProbabilisticExecutionVenueProvider probabilisticExecutionVenueProvider;
    private final ConsolidatedOrderBook consolidatedOrderBook;
    private int totalRouted;
    private final Map<Integer, Order> idOrderMap;

    public SORSupport(OrderIdService orderIdService, ProbabilisticExecutionVenueProvider probabilisticExecutionVenueProvider, ConsolidatedOrderBook consolidatedOrderBook)
    {
        this.orderIdService = orderIdService;
        this.probabilisticExecutionVenueProvider = probabilisticExecutionVenueProvider;
        this.consolidatedOrderBook = consolidatedOrderBook;
        idOrderMap = new ConcurrentHashMap<>();
        totalRouted = 0;
    }

    public void sendToFixOut(VenuePropertyPair<Order> venuePropertyPair)
    {
    }

    private synchronized void updateTotalRouted(int adjustment)
    {
        totalRouted += adjustment;
    }

    protected abstract void onDoneCreatingChildOrders();

    protected abstract void onNewChildOrder(VenuePropertyPair<Order> venuePropertyPair);

    public void sliceIntoChildren(Order order, ExecType execType)
    {
        Iterator<VenuePropertyPair<Integer>> venueChildOrderIterator = currentVenueTargets.iterator();
        for (VenuePropertyPair<Integer> venueOrderPair = venueChildOrderIterator.next();
             venueChildOrderIterator.hasNext() && totalRouted <= order.getQuantity();
             venueOrderPair = venueChildOrderIterator.next())
        {
            int targetLeaves = venueOrderPair.getVal();
            int remaining = order.getQuantity() - totalRouted;
            int childQuantity = Math.min(targetLeaves, remaining);
            int id = orderIdService.generateId();
            Order child = order.createChild(childQuantity, execType, id);
            idOrderMap.put(child.getId(), child);
            Venue venue = venueOrderPair.getVenue();
            onNewChildOrder(new VenuePropertyPair<>(child, venue));
            totalRouted += childQuantity;
        }
    }

    @Override
    public void route(Order order, RoutingConfig routingConfig)
    {
        if (order.isTerminal())
        {
            throw new RuntimeException("Cannot route a terminal order!");
        }
        createSweepChildOrders(order, routingConfig);
        onDoneCreatingChildOrders();
        if (orderStillMarketable(order, totalRouted))
        {
            createPostChildOrders(order, routingConfig);
            onDoneCreatingChildOrders();
        }
    }

    private void createPostChildOrders(Order order, RoutingConfig routingConfig)
    {
        List<VenuePropertyPair<Double>> venueExecutionProbabilityPairs = probabilisticExecutionVenueProvider.getVenueExecutionProbabilityPairs(order.getSymbol(), RoutingStage.POST, routingConfig);
        for (VenuePropertyPair<Double> venueExecutionPair : venueExecutionProbabilityPairs)
        {
            double executionProbability = venueExecutionPair.getVal();
            int targetLeaves = (int) (order.getQuantity() * executionProbability);
            currentVenueTargets.add(new VenuePropertyPair<>(targetLeaves, venueExecutionPair.getVenue()));
        }
        sliceIntoChildren(order, ExecType.GFD);
    }

    private void createSweepChildOrders(Order order, RoutingConfig routingConfig)
    {
        LiquidityQuery query = new LiquidityQuery(routingConfig.getSweepType(), routingConfig.getRoutingCountry());
        currentVenueTargets = consolidatedOrderBook.getOutstandingSharesPerVenue(query, order);
        sliceIntoChildren(order, ExecType.IOC);
    }

    private boolean orderStillMarketable(Order order, int totalRouted)
    {
        return totalRouted != order.getQuantity();
    }


    @Override
    public void onExecution(int id, int shares)
    {

    }

    @Override
    public void onCancel(int id)
    {
        Order order = idOrderMap.get(id);
        updateTotalRouted(-order.getLeaves());
    }

    @Override
    public void onReplace(int id, Order newOrder)
    {
        Order order = idOrderMap.get(id);
        order.get
    }
}
