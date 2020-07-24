package com.h2o_execution.smart_order_router.core;

import com.h2o_execution.smart_order_router.domain.Order;
import com.h2o_execution.smart_order_router.domain.Venue;
import com.h2o_execution.smart_order_router.market_access.OrderManager;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

import static java.lang.Thread.currentThread;

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
        toRoute = new ArrayList<>();
    }

    @Override
    protected void onNewChildOrder(VenuePropertyPair<Order> venuePropertyPair) {
        toRoute.add(venuePropertyPair);
    }

    @Override
    protected void onDoneCreatingChildOrders() {
        int threadCount = Math.min(MAX_PARALLELISM, toRoute.size());
        int max = getMaxLatency();
        CountDownLatch countDownLatch = new CountDownLatch(threadCount);
        ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(threadCount);
        try {
            for (VenuePropertyPair<Order> vpp : toRoute) {
                int latencyAdjustment = max - vpp.getVenue().getAvgLatency();
                Runnable sendOrderTask = () -> {
                    orderManager.sendOrder(vpp.getVenue(), vpp.getVal(), this);
                    countDownLatch.countDown();
                };
                scheduledExecutorService.schedule(sendOrderTask, latencyAdjustment, TimeUnit.MILLISECONDS);
                countDownLatch.await();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            scheduledExecutorService.shutdown();
        }
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
