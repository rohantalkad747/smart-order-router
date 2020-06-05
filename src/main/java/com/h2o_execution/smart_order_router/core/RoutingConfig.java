package com.h2o_execution.smart_order_router.core;

import com.h2o_execution.smart_order_router.domain.Generation;
import com.h2o_execution.smart_order_router.domain.Venue;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

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
    private RoutingCountry routingCountry;
    private List<Venue> excludedVenues;
    private Venue.Type sweepType;
    private Venue.Type postType;
}

