package com.h2o_execution.smart_order_router.core;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Rank {
    private double venueTradingCost;
    private double routerHistoricTradingVolume;
    private double marketHistoricTradingVolume;
    private double immediateTradingVolume;
    private double priceImprovementIndicator;

    public double calculate() {
        return (venueTradingCost * 0.15) +
                (routerHistoricTradingVolume * 0.2) +
                (marketHistoricTradingVolume * 0.1) +
                (priceImprovementIndicator * 0.15) +
                (immediateTradingVolume * 0.4);
    }
}
