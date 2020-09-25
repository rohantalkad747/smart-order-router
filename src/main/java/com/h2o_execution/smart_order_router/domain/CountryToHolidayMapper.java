package com.h2o_execution.smart_order_router.domain;

import com.h2o_execution.smart_order_router.core.Country;

import java.time.MonthDay;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public interface CountryToHolidayMapper {
    Optional<Map<Country, Set<MonthDay>>> getMapping();
}
