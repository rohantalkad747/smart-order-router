package com.h2o_execution.smart_order_router.market_access;


import com.h2o_execution.smart_order_router.core.OrderEventsListener;
import com.h2o_execution.smart_order_router.core.Router;
import com.h2o_execution.smart_order_router.domain.Order;
import com.h2o_execution.smart_order_router.domain.OrderType;
import com.h2o_execution.smart_order_router.domain.Venue;
import lombok.extern.slf4j.Slf4j;
import quickfix.field.*;
import quickfix.fix42.NewOrderSingle;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Slf4j
public class OrderManager implements IOrderManager, OrderEventsListener {
    private final Map<String, Router> activeRouters;
    private final FIXMessageMediator fixMessageMediator;

    public OrderManager(FIXMessageMediator fixMessageMediator) {
        this.fixMessageMediator = fixMessageMediator;
        this.activeRouters = new HashMap<>();
    }

    private NewOrderSingle buildNewOrderSingle(Order order) {
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
        if (order.getOrderType() == OrderType.LIMIT) {
            newOrderSingle.set(new Price(order.getLimitPrice()));
        }
        return newOrderSingle;
    }

    @Override
    public void sendOrder(Venue v, Order order, Router router) {
        NewOrderSingle newOrderSingle = buildNewOrderSingle(order);
        try {
            fixMessageMediator.fireNewOrderEvent(v, newOrderSingle, this);
            activeRouters.put(order.getClientOrderId(), router);
        } catch (Exception e) {
            log.error("Exception during sending order", e);
        }
    }

    @Override
    public void onExecution(String clientOrderId, int shares) {
        activeRouters.get(clientOrderId).onExecution(clientOrderId, shares);
    }

    @Override
    public void onReject(String clientOrderId) {
        activeRouters.get(clientOrderId).onReject(clientOrderId);
    }
}
