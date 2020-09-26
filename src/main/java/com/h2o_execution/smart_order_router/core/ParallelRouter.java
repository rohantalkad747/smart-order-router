package com.h2o_execution.smart_order_router.core;

import com.google.common.collect.Lists;
import com.h2o_execution.smart_order_router.domain.Order;
import com.h2o_execution.smart_order_router.domain.Venue;
import com.h2o_execution.smart_order_router.market_access.OrderManager;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * This is a parallel smart order implementation that makes dispatch time adjustments in an effort
 * to allow orders to reach their venues at roughly the same time.
 */
@Slf4j
public class ParallelRouter extends AbstractRouter {
    private static final int MAX_PARALLELISM = Runtime.getRuntime().availableProcessors();
    private final List<VenuePropertyPair<Order>> toRoute;

    /**
     * @param orderManager
     * @param orderIdService
     * @param probabilisticExecutionVenueProvider
     * @param consolidatedOrderBook
     * @param routingConfig
     */
    public ParallelRouter(OrderManager orderManager, OrderIdService orderIdService, ProbabilisticExecutionVenueProvider probabilisticExecutionVenueProvider, ConsolidatedOrderBook consolidatedOrderBook, RoutingConfig routingConfig) {
        super(orderIdService, orderManager, probabilisticExecutionVenueProvider, consolidatedOrderBook, routingConfig);
        toRoute = Lists.newArrayList();
    }

    @Override
    protected void onNewChildOrder(VenuePropertyPair<Order> venuePropertyPair) {
        toRoute.add(venuePropertyPair);
    }

    @Override
    protected void onDoneCreatingChildOrders() {
        int threadCount = getThreadCount();
        int max = getMaxLatency();
        CountDownLatch countDownLatch = new CountDownLatch(threadCount);
        ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(threadCount);
        tryRoute(max, countDownLatch, scheduledExecutorService);
    }

    private void tryRoute(int max, CountDownLatch countDownLatch, ScheduledExecutorService scheduledExecutorService) {
        try {
            doRoute(max, countDownLatch, scheduledExecutorService);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            scheduledExecutorService.shutdown();
        }
    }

    private int getThreadCount() {
        return Math.min(MAX_PARALLELISM, toRoute.size());
    }

    private void doRoute(int max, CountDownLatch countDownLatch, ScheduledExecutorService scheduledExecutorService) throws InterruptedException {
        for (VenuePropertyPair<Order> vpp : toRoute) {
            scheduleRouteTask(max, countDownLatch, scheduledExecutorService, vpp);
        }
        countDownLatch.await();
    }

    private void scheduleRouteTask(int max, CountDownLatch countDownLatch, ScheduledExecutorService scheduledExecutorService, VenuePropertyPair<Order> vpp) throws InterruptedException {
        int latencyAdjustment = getLatencyAdjustment(max, vpp);
        doSchedule(countDownLatch, scheduledExecutorService, vpp, latencyAdjustment);
    }

    private void doSchedule(CountDownLatch countDownLatch, ScheduledExecutorService scheduledExecutorService, VenuePropertyPair<Order> vpp, int latencyAdjustment) {
        Runnable sendOrderTask = createRouteTask(countDownLatch, vpp);
        scheduledExecutorService.schedule(sendOrderTask, latencyAdjustment, TimeUnit.MILLISECONDS);
    }

    @NotNull
    private Runnable createRouteTask(CountDownLatch countDownLatch, VenuePropertyPair<Order> vpp) {
        return () -> {
            orderManager.sendOrder(vpp.getVenue(), vpp.getVal(), this);
            countDownLatch.countDown();
        };
    }

    private int getLatencyAdjustment(int max, VenuePropertyPair<Order> vpp) {
        return max - vpp.getVenue().getAvgLatency();
    }


    private int getMaxLatency() {
        return routes
                .keySet()
                .stream()
                .map(Venue::getAvgLatency)
                .max(Integer::compareTo)
                .orElse(0);
    }
}
