package com.h2o_execution.smart_order_router.core;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class LiquidityQuery
{
    Venue.Type type;
    RoutingCountry country;
}
