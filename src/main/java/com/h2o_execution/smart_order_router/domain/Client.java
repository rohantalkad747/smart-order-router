package com.h2o_execution.smart_order_router.domain;

import com.h2o_execution.smart_order_router.core.Currency;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Client
{
    private String id;
    private Type type;
    private Inbox inbox;
    private Map<Currency, Double> balances;

    public enum Type
    {
        TRADING_DESK,
        MARKET_MAKER,
        PROP_TRADER,
        FUND
    }
}
