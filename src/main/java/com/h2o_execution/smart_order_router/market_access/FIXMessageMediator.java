package com.h2o_execution.smart_order_router.market_access;

import com.h2o_execution.smart_order_router.domain.Venue;
import quickfix.SessionID;
import quickfix.fix42.ExecutionReport;
import quickfix.fix42.NewOrderSingle;
import quickfix.fix42.Reject;

public class FIXMessageMediator
{
    private FIXGateway gateway;
    private OrderManager orderManager;
    private VenueSessionRegistry venueSessionRegistry;

    public void fireConnectionEvent(SessionID sessionID)
    {
        venueSessionRegistry.onConnect(sessionID);
    }

    public void fireNewOrderEvent(Venue venue, NewOrderSingle order)
    {
        SessionID session = venueSessionRegistry.getSession(venue);
        gateway.sendMessage(session, order);
    }

    public void fireReceiveExecutionReport(ExecutionReport executionReport)
    {

    }

    public void fireRejectionEvent(Reject reject)
    {

    }
}
