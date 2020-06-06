package com.h2o_execution.smart_order_router.domain;

import lombok.*;
import quickfix.field.Side;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Order implements Cloneable
{
    private String symbol;
    private String clientOrderId;
    private TimeInForce timeInForce;
    private Currency currency;
    private Side side;
    private int quantity;
    @Setter(AccessLevel.NONE)
    private int cumulativeQuantity;
    private int limitPrice;
    private OrderType orderType;

    public synchronized void updateCumulativeQuantity(int newQuantity)
    {
        cumulativeQuantity += newQuantity;
    }

    public Order createChild(int childQuantity, TimeInForce timeInForce, String newId)
    {
        return Order
                .builder()
                .symbol(symbol)
                .currency(currency)
                .clientOrderId(newId)
                .side(side)
                .quantity(childQuantity)
                .cumulativeQuantity(0)
                .limitPrice(limitPrice)
                .timeInForce(timeInForce)
                .orderType(orderType)
                .build();
    }


    public int getLeaves()
    {
        return quantity - cumulativeQuantity;
    }

    public boolean isTerminal()
    {
        return getLeaves() == 0;
    }
}
