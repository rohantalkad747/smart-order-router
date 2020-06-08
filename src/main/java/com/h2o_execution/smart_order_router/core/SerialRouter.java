package com.h2o_execution.smart_order_router.core;

import com.h2o_execution.smart_order_router.domain.Order;
import com.h2o_execution.smart_order_router.market_access.OrderManager;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SerialRouter extends AbstractRouter
{

    public SerialRouter(OrderManager orderManager, OrderIdService orderIdService, ProbabilisticExecutionVenueProvider probabilisticExecutionVenueProvider, ConsolidatedOrderBook consolidatedOrderBook, RoutingConfig routingConfig)
    {
        super(orderIdService, orderManager, probabilisticExecutionVenueProvider, consolidatedOrderBook, routingConfig);
    }

    @Override
    protected void onDoneCreatingChildOrders()
    {
        log.info("Done serially routing");
    }

    @Override
    protected void onNewChildOrder(VenuePropertyPair<Order> venuePropertyPair)
    {
        orderManager.sendOrder(venuePropertyPair.getVenue(), venuePropertyPair.getVal(), this);
    }
}
