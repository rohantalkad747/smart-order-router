package com.h2o_execution.smart_order_router.market_access;

import com.h2o_execution.smart_order_router.domain.Venue;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import quickfix.FieldNotFound;
import quickfix.SessionID;
import quickfix.field.ClOrdID;
import quickfix.fix42.ExecutionReport;
import quickfix.fix42.NewOrderSingle;
import quickfix.fix42.Reject;

@Slf4j
@AllArgsConstructor
public class FIXMessageMediator
{
    private final FIXGateway gateway;
    private final OrderManager orderManager;
    private final VenueSessionRegistry venueSessionRegistry;

    public void fireConnectEvent(SessionID sessionID)
    {
        venueSessionRegistry.onConnect(sessionID);
    }

    public void fireDisconnectEvent(SessionID sessionID)
    {
        venueSessionRegistry.onDisconnect(sessionID);
    }

    public void fireNewOrderEvent(Venue venue, NewOrderSingle order)
    {
        SessionID session = venueSessionRegistry.getSession(venue);
        gateway.sendMessage(session, order);
    }

    public void fireReceiveExecutionReport(ExecutionReport executionReport)
    {
        try
        {
            String clOrdId = executionReport.getClOrdID().getValue();
            int amount = (int) executionReport.getLastShares().getValue();
            orderManager.onExecution(clOrdId, amount);
        }
        catch (FieldNotFound fieldNotFound)
        {
            log.error("Exception thrown during execution report event", fieldNotFound);
        }
    }

    public void fireRejectionEvent(Reject reject)
    {
        try
        {
            orderManager.onReject(reject.getString(ClOrdID.FIELD));
        }
        catch (FieldNotFound fieldNotFound)
        {
            log.error("Exception thrown during rejection event", fieldNotFound);
        }
    }
}
