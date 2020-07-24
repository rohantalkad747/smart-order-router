package com.h2o_execution.smart_order_router.core;

import com.h2o_execution.smart_order_router.domain.Venue;

import java.util.List;

public interface ProbabilisticExecutionVenueProvider {
    List<VenuePropertyPair<Double>> getVenueExecutionProbabilityPairs(String symbol, RoutingStage stage, RoutingConfig routingConfig);

    void addVenue(Venue v);

    void removeVenue(Venue v);
}
