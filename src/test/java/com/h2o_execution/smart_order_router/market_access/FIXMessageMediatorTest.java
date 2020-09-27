package com.h2o_execution.smart_order_router.market_access;

import com.h2o_execution.smart_order_router.core.OrderEventsListener;
import com.h2o_execution.smart_order_router.domain.Venue;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import quickfix.Session;
import quickfix.SessionID;
import quickfix.field.ClOrdID;
import quickfix.fix42.Message;
import quickfix.fix42.NewOrderSingle;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class FIXMessageMediatorTest {

    FIXGateway fixGateway = Mockito.mock(FIXGateway.class);
    VenueSessionRegistry venueSessionRegistry = Mockito.mock(VenueSessionRegistry.class);
    FIXMessageMediator fixMessageMediator = new FIXMessageMediator(fixGateway, venueSessionRegistry);

    @Test
    void fireConnectEvent() {
        SessionID sessionId = Mockito.mock(SessionID.class);
        // When
        fixMessageMediator.fireConnectEvent(sessionId);

        // Then
        verify(venueSessionRegistry).onConnect(sessionId);
    }


    @Test
    void fireNewOrderEvent() throws Exception {
        // Given
        NewOrderSingle newOrderSingle = mock(NewOrderSingle.class);
        SessionID sessionId = Mockito.mock(SessionID.class);
        Venue mockVenue = mock(Venue.class);
        when(newOrderSingle.getClOrdID()).thenReturn(new ClOrdID("test"));
        when(venueSessionRegistry.getSession(mockVenue)).thenReturn(sessionId);

        // When
        fixMessageMediator.fireNewOrderEvent(mockVenue, newOrderSingle, mock(OrderEventsListener.class));

        // Then
        verify(fixGateway).sendMessage(sessionId, newOrderSingle);
    }

}