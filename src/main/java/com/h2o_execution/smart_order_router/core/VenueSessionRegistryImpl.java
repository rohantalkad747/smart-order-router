package com.h2o_execution.smart_order_router.core;

import com.h2o_execution.smart_order_router.domain.Venue;
import com.h2o_execution.smart_order_router.market_access.FIXGateway;
import com.h2o_execution.smart_order_router.market_access.VenueSessionRegistry;
import quickfix.Session;
import quickfix.SessionID;

import java.util.List;
import java.util.Map;

public class VenueSessionRegistryImpl implements VenueSessionRegistry {

    private FIXGateway fixGateway;
    private Map<Venue, Session> sessionMap;

    @Override
    public SessionID getSession(Venue venue) {
        return null;
    }

    @Override
    public void onConnect(SessionID sessionID) {
    }

    @Override
    public void createSessions(List<Venue> venues) {

    }

    @Override
    public void onDisconnect(SessionID sessionID) {

    }
}
