package com.h2o_execution.smart_order_router.core;

import com.h2o_execution.smart_order_router.domain.Generation;
import com.h2o_execution.smart_order_router.domain.Venue;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Currency;
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
    private Map<Currency, Double> availableCapital;
}

