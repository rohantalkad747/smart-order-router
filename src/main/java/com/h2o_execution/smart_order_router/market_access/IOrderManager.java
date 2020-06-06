package com.h2o_execution.smart_order_router.market_access;

import com.h2o_execution.smart_order_router.core.Router;
import com.h2o_execution.smart_order_router.domain.Order;
import com.h2o_execution.smart_order_router.domain.OrderType;
import com.h2o_execution.smart_order_router.domain.Venue;
import quickfix.field.*;
import quickfix.fix42.NewOrderSingle;

import java.time.LocalDateTime;

public interface IOrderManager
{
    void sendOrder(Venue v, Order order, Router router);

    default NewOrderSingle buildNewOrderSingle(Order order)
    {
        NewOrderSingle newOrderSingle = new NewOrderSingle
        (
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
        return newOrderSingle;
    }
}
