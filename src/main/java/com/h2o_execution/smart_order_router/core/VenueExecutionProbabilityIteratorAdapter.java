package com.h2o_execution.smart_order_router.core;

import com.h2o_execution.smart_order_router.domain.Order;

import java.util.Iterator;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class VenueExecutionProbabilityIteratorAdapter implements Iterator<VenuePropertyPair<Integer>>
{
    private Iterator<VenuePropertyPair<Double>> delegate;
    private Order order;

    @Override
    public boolean hasNext()
    {
        return delegate.hasNext();
    }

    @Override
    public VenuePropertyPair<Integer> next()
    {
        VenuePropertyPair<Double> venueExecutionPair = delegate.next();
        double executionProbability = venueExecutionPair.getVal();
        int targetLeaves = (int) (order.getQuantity() * executionProbability);
        return new VenuePropertyPair<>(targetLeaves, venueExecutionPair.getVenue());
    }
}
