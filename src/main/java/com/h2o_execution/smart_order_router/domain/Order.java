package com.h2o_execution.smart_order_router.domain;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Order implements Cloneable
{
    private Venue venue;
    private String symbol;
    private String clientOrderId;
    private TimeInForce timeInForce;
    private Currency currency;
    private Side side;
    private volatile int quantity;
    private volatile int cumulativeQuantity;
    private volatile double limitPrice;
    private OrderType orderType;

    public synchronized void updateCumulativeQuantity(int newQuantity)
    {
        cumulativeQuantity += newQuantity;
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
