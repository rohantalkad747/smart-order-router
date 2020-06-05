package com.h2o_execution.smart_order_router.market_access;

import com.h2o_execution.smart_order_router.domain.Venue;
import quickfix.SessionID;

import java.util.List;

/**
 * Managed service for venue connectivity and FIX session creation/retrieval.
 */
public interface VenueSessionRegistry
{
    SessionID getSession(Venue venue);

    void onConnection(Venue venue);

    void connect(List<Venue> venueList);

    void onDisconnect(Venue venue);
}
