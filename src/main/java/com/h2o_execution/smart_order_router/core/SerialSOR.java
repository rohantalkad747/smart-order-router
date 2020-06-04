package com.h2o_execution.smart_order_router.core;

import com.h2o_execution.smart_order_router.domain.Order;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SerialSOR extends SORSupport
{

    public SerialSOR(ProbabilisticExecutionVenueProvider probabilisticExecutionVenueProvider, ConsolidatedOrderBook consolidatedOrderBook)
    {
        super(orderIdService, probabilisticExecutionVenueProvider, consolidatedOrderBook);
    }

    @Override
    protected void onDoneCreatingChildOrders()
    {
        log.info("Done serially routing");
    }

    @Override
    protected void onNewChildOrder(VenuePropertyPair<Order> venuePropertyPair)
    {
        log.info("Sending to FIX engine");
        sendToFixOut(venuePropertyPair);
    }
}
