package com.h2o_execution.smart_order_router.core;

import com.h2o_execution.smart_order_router.domain.Order;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

/**
 * This is a parallel smart order implementation that makes dispatch time adjustments in an effort
 * to allow orders to reach their venues at roughly the same time.
 */
@Slf4j
public class ParallelSOR extends SORSupport
{
    private static final int MAX_PARALLELISM = Runtime.getRuntime().availableProcessors();
    private final List<VenuePropertyPair<Order>> toRoute;

    public ParallelSOR(ProbabilisticExecutionVenueProvider probabilisticExecutionVenueProvider, ConsolidatedOrderBook consolidatedOrderBook)
    {
        super(orderIdService, probabilisticExecutionVenueProvider, consolidatedOrderBook);
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
        List<Long> latencyAdjustments = getLatencyAdjustments();
        CountDownLatch countDownLatch = new CountDownLatch(threadCount);
        for (int i = 0; i < toRoute.size(); i++)
        {
            VenuePropertyPair<Order> vpp = toRoute.get(i);
            long latencyAdjustment = latencyAdjustments.get(i);
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
        }
    }

    private Callable<Void> buildChildOrderSendTask(CountDownLatch countDownLatch, VenuePropertyPair<Order> vpp, long latencyAdjustment)
    {
        return () ->
        {
            try
            {
                Thread.sleep(latencyAdjustment);
                sendToFixOut(vpp);
                countDownLatch.countDown();
            }
            catch (InterruptedException e)
            {
                Thread.currentThread().interrupt();
                log.error("Thread interrupted while performing latency adjustment", e);
            }
            return null;
        };
    }

    private List<Long> getLatencyAdjustments()
    {
        List<Venue> venues = currentVenueTargets
                .stream()
                .map(VenuePropertyPair::getVenue)
                .collect(Collectors.toList());
        List<Long> adjustments = new ArrayList<>();
        long max = 0;
        for (Venue venue : venues)
        {
            long latency = venue.getAvgLatency();
            if (latency > max)
            {
                max = latency;
            }
        }
        for (Venue venue : venues)
        {
            long thisVenueLatency = venue.getAvgLatency();
            long adjustment = max - thisVenueLatency;
            adjustments.add(adjustment);
        }
        return adjustments;
    }
}
