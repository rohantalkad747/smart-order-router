package com.h2o_execution.smart_order_router.market_access;

import com.h2o_execution.smart_order_router.domain.Venue;
import quickfix.SessionID;

import java.util.List;

/**
 * Managed service for venue connectivity
 *
 * @author Rohan
 */
public interface VenueSessionRegistry {
    SessionID getSession(Venue venue);

    void onConnect(SessionID sessionID);

    void createSessions(List<Venue> venues);

    void onDisconnect(SessionID sessionID);
}
