package com.h2o_execution.smart_order_router.market_access;


import com.h2o_execution.smart_order_router.core.Router;
import com.h2o_execution.smart_order_router.domain.Order;
import com.h2o_execution.smart_order_router.domain.OrderType;
import com.h2o_execution.smart_order_router.domain.Venue;
import quickfix.field.*;
import quickfix.fix42.NewOrderSingle;

import java.time.LocalDateTime;
import java.util.Map;

public class RouterOrderManagementService
{
    private FIXGateway delegate;
    private Map<String, Router> activeRouters;
    private OrderActionsMediator orderActionsMediator;

    public void sendOrder(Venue v, Order order, Router router)
    {
        NewOrderSingle newOrderSingle = new NewOrderSingle(
                new ClOrdID(order.getClientOrderId()),
                new HandlInst(HandlInst.AUTOMATED_EXECUTION_ORDER_PRIVATE_NO_BROKER_INTERVENTION),
                new Symbol(order.getSymbol()),
                new Side(order.getSide().getValue()),
                new TransactTime(LocalDateTime.now()),
                new OrdType(order.getOrderType().getValue())
        );
        newOrderSingle.set(new TimeInForce(order.getTimeInForce().getValue()));
        newOrderSingle.set(new Currency(order.getCurrency().name()));
        if (order.getOrderType() == OrderType.LIMIT)
        {
            newOrderSingle.set(new Price(order.getLimitPrice()));
        }
        orderActionsMediator.fireNewSingleOrderEvent(v, newOrderSingle);
        activeRouters.put(order.getClientOrderId(), router);
    }



}
