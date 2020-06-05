package com.h2o_execution.smart_order_router.market_access;

import com.h2o_execution.smart_order_router.domain.Venue;
import quickfix.SessionID;
import quickfix.fix42.NewOrderSingle;
import quickfix.fix42.Reject;

public class OrderActionsMediator
{
    private FIXGateway gateway;
    private RouterOrderManagementService venueAccessService;
    private VenueSessionRegistry venueSessionRegistry;

    public void fireNewSingleOrderEvent(Venue venue, NewOrderSingle order)
    {
        SessionID session = venueSessionRegistry.getSession(venue);
        gateway.sendMessage(session, order);
    }

    public void fireRejectionEvent(Reject reject)
    {

    }
}
