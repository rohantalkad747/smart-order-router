package com.h2o_execution.smart_order_router.core;

import com.h2o_execution.smart_order_router.domain.Order;
import lombok.AllArgsConstructor;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;

@AllArgsConstructor
public class H2OSOR implements SOR
{
    private ProbabilisticExecutionVenueProvider probabilisticExecutionVenueProvider;
    private ConsolidatedOrderBook consolidatedOrderBook;
    private int totalRouted;
    private ExecutorService executorService;
    private Map<Integer, Order> idOrderMap;

    private void sendToFixOut(Venue venue, Order order)
    {
    }



    public void sliceIntoChildren(Order order, Iterator<VenuePropertyPair<Integer>> venueChildOrderIterator)
    {
        List<Runnable> runnables = new ArrayList<>();
        for (VenuePropertyPair<Integer> venueOrderPair = venueChildOrderIterator.next();
             venueChildOrderIterator.hasNext() && totalRouted <= order.getQuantity();
             venueOrderPair = venueChildOrderIterator.next())
        {
            int targetLeaves = venueOrderPair.getVal();
            int remaining = order.getQuantity() - totalRouted;
            int childQuantity = Math.min(targetLeaves, remaining);
            Order child = order.createChild(childQuantity, ExecType.GTC);
            sendToFixOut(venueOrderPair.getVenue(), child);
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
        sweep(order, routingConfig);
        if (orderStillMarketable(order, totalRouted))
        {
            post(order, routingConfig);
        }
    }

    private void post(Order order, RoutingConfig routingConfig)
    {
        List<VenuePropertyPair<Double>> venueExecutionProbabilityPairs = probabilisticExecutionVenueProvider.getVenueExecutionProbabilityPairs(order.getSymbol(), RoutingStage.POST, routingConfig);
        Iterator<VenuePropertyPair<Double>> executionIterator = venueExecutionProbabilityPairs.iterator();
        VenueExecutionProbabilityIteratorAdapter childOrderIterator = new VenueExecutionProbabilityIteratorAdapter(executionIterator, order);
        sliceIntoChildren(order, childOrderIterator);
    }

    private void sweep(Order order, RoutingConfig routingConfig)
    {
        LiquidityQuery query = new LiquidityQuery(routingConfig.getSweepType(), routingConfig.getGetCountryRoutingConfig());
        List<VenuePropertyPair<Integer>> venueExecutionPairs = consolidatedOrderBook.getOutstandingSharesPerVenue(query, order);
        Iterator<VenuePropertyPair<Integer>> iterator = venueExecutionPairs.iterator();
        sliceIntoChildren(order, iterator);
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

    }

    @Override
    public void onReplace(int id, Order newOrder)
    {

    }
}
