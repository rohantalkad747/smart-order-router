package com.h2o_execution.smart_order_router.domain;

import com.h2o_execution.smart_order_router.core.ExecType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Currency;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Order implements Cloneable
{
    public enum Side { BUY, SELL }
    public enum Status { NEW, PARTIAL, FILLED, CANCELLED, REJECTED, EXPIRED }

    private String symbol;
    private int id;
    private ExecType execType;
    private Currency currency;
    private Status status;
    private Side side;
    private int quantity;
    private int cumulativeQuantity;
    private int limitPrice;
    private long entryTime;
    private long updateTime;
    private OrderType orderType;

    public Order createChild(int childQuantity, ExecType execType, int newId)
    {
        return Order
                .builder()
                .symbol(symbol)
                .currency(currency)
                .status(Status.NEW)
                .side(side)
                .id(newId)
                .quantity(childQuantity)
                .cumulativeQuantity(0)
                .limitPrice(limitPrice)
                .entryTime(entryTime)
                .execType(execType)
                .updateTime(updateTime)
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
