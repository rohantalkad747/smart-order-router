package com.h2o_execution.smart_order_router.core;

import com.h2o_execution.smart_order_router.domain.Currency;
import com.h2o_execution.smart_order_router.domain.Generation;
import com.h2o_execution.smart_order_router.domain.Venue;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * This data structure holds the configuration for a order route.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoutingConfig
{
    private Generation generation;
    private Set<Country> countrySet;
    private List<Venue> excludedVenues;
    private Venue.Type sweepType;
    private Venue.Type postType;
    private Currency baseCurrency;
    private Map<Currency, Double> availableCapital;

    public Currency getBaseCurrency()
    {
        if ( baseCurrency == null )
        {
            baseCurrency = availableCapital
                    .entrySet()
                    .stream()
                    .max(Comparator.comparingDouble(Map.Entry::getValue))
                    .get()
                    .getKey();
        }
        return baseCurrency;
    }
}

