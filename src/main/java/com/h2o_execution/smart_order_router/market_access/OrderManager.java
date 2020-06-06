package com.h2o_execution.smart_order_router.market_access;


import com.h2o_execution.smart_order_router.core.Router;
import com.h2o_execution.smart_order_router.core.OrderEventsListener;
import com.h2o_execution.smart_order_router.domain.Order;
import com.h2o_execution.smart_order_router.domain.Venue;
import quickfix.fix42.NewOrderSingle;

import java.util.Map;

public class OrderManager implements IOrderManager, OrderEventsListener
{
    private Map<String, Router> activeRouters;
    private FIXMessageMediator fixMessageMediator;

    @Override
    public void sendOrder(Venue v, Order order, Router router)
    {
        NewOrderSingle newOrderSingle = buildNewOrderSingle(order);
        fixMessageMediator.fireNewOrderEvent(v, newOrderSingle);
        activeRouters.put(order.getClientOrderId(), router);
    }

    @Override
    public void onExecution(String clientOrderId, int shares)
    {
        activeRouters.get(clientOrderId).onExecution(clientOrderId, shares);
    }

    @Override
    public void onReject(String clientOrderId)
    {
        activeRouters.get(clientOrderId).onReject(clientOrderId);
    }


}
