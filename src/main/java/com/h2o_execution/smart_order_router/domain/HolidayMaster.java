package com.h2o_execution.smart_order_router.domain;

import com.h2o_execution.smart_order_router.core.Country;

import java.time.MonthDay;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class HolidayMaster
{
    // TODO: load these from a configuration file
    private static List<MonthDay> AMERICAN_HOLIDAYS;
    private static List<MonthDay> BRITISH_HOLIDAYS;
    private static List<MonthDay> CANADIAN_HOLIDAYS;

    private Map<MonthDay, Set<Country>> holidays;

    public boolean isHoliday(Venue venue)
    {
        MonthDay monthDay = MonthDay.now();
        Set<Country> countries = holidays.get(monthDay);
        Country country = venue.getCountry();
        return countries != null && countries.contains(country);
    }
}
