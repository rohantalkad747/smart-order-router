package com.h2o_execution.smart_order_router.domain;

import com.h2o_execution.smart_order_router.core.Country;
import com.h2o_execution.smart_order_router.core.Rank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Venue
{

    private Name name;
    private ZoneId timeZone;
    private Country country;
    private Currency currency;
    private Type type;
    private List<String> symbols;
    private Map<String, Rank> symbolRankMap;
    private HolidayMaster holidayMaster;
    private long avgLatency;
    private Bell open;
    private Bell close;

    public boolean isAvailable()
    {
        if (holidayMaster.isHoliday(this))
        {
            return false;
        }
        ZonedDateTime zonedDateTime = LocalDateTime.now().atZone(timeZone);
        return  beforeOpeningBell(zonedDateTime) ||
                afterClosingBell(zonedDateTime);

    }

    private boolean afterClosingBell(ZonedDateTime localDateTime)
    {
        return localDateTime.getHour() > close.hours && localDateTime.getMinute() > close.minutes;
    }

    private boolean beforeOpeningBell(ZonedDateTime localDateTime)
    {
        return localDateTime.getHour() < close.hours && localDateTime.getMinute() < close.minutes;
    }

    public enum Type
    {LIT, DARK}

    @AllArgsConstructor
    @Data
    public static class Bell
    {
        private int hours;
        private int minutes;
    }
}
