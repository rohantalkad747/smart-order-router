package com.h2o_execution.smart_order_router.core;

import com.h2o_execution.smart_order_router.domain.Order;
import com.h2o_execution.smart_order_router.domain.Venue;
import com.h2o_execution.smart_order_router.market_access.OrderManager;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * This is a parallel smart order implementation that makes dispatch time adjustments in an effort
 * to allow orders to reach their venues at roughly the same time.
 */
@Slf4j
public class ParallelRouter extends AbstractRouter
{
    private static final int MAX_PARALLELISM = Runtime.getRuntime().availableProcessors();
    private final List<VenuePropertyPair<Order>> toRoute;

    public ParallelRouter(OrderManager orderManager, OrderIdService orderIdService, ProbabilisticExecutionVenueProvider probabilisticExecutionVenueProvider, ConsolidatedOrderBook consolidatedOrderBook, RoutingConfig routingConfig)
    {
        super(orderIdService, orderManager, probabilisticExecutionVenueProvider, consolidatedOrderBook, routingConfig);
        toRoute = new ArrayList<>();
    }

    @Override
    protected void onNewChildOrder(VenuePropertyPair<Order> venuePropertyPair)
    {
        toRoute.add(venuePropertyPair);
    }

    @Override
    protected void onDoneCreatingChildOrders()
    {
        int threadCount = Math.min(MAX_PARALLELISM, toRoute.size());
        List<Callable<Void>> sendTasks = new ArrayList<>();
        long max = getMaxLatency();
        CountDownLatch countDownLatch = new CountDownLatch(threadCount);

        for (VenuePropertyPair<Order> vpp : toRoute)
        {
            long latencyAdjustment = max - vpp.getVenue().getAvgLatency();
            Callable<Void> sendTask = buildChildOrderSendTask(countDownLatch, vpp, latencyAdjustment);
            sendTasks.add(sendTask);
        }
        sendOrdersInParallel(threadCount, sendTasks, countDownLatch);
    }

    private void sendOrdersInParallel(int thCount, List<Callable<Void>> callables, CountDownLatch countDownLatch)
    {
        ExecutorService executorService = Executors.newFixedThreadPool(thCount);
        try
        {
            executorService.invokeAll(callables);
            countDownLatch.await();
        }
        catch (InterruptedException e)
        {
            Thread.currentThread().interrupt();
            log.error("Thread interrupted while sending orders in parallel", e);
        }
        finally
        {
            executorService.shutdown();
            log.info("Thread pool shutdown for SOR");
        }
    }

    private Callable<Void> buildChildOrderSendTask(CountDownLatch countDownLatch, VenuePropertyPair<Order> vpp, long latencyAdjustment)
    {
        return () ->
        {
            try
            {
                Thread.sleep(latencyAdjustment);
                orderManager.sendOrder(vpp.getVenue(), vpp.getVal(), this);
                countDownLatch.countDown();
            }
            catch (InterruptedException e)
            {
                Thread.currentThread().interrupt();
                log.error("Thread interrupted in order send task", e);
            }
            return null;
        };
    }

    private long getMaxLatency()
    {
        long max = 0;
        for (Venue venue : routes.keySet())
        {
            long latency = venue.getAvgLatency();
            if (latency > max)
            {
                max = latency;
            }
        }
        return max;
    }
}
