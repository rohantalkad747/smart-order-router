package com.h2o_execution.smart_order_router.domain;

import com.google.common.collect.Maps;
import com.h2o_execution.smart_order_router.core.Country;
import com.h2o_execution.smart_order_router.core.Rank;
import lombok.*;

import java.time.LocalDateTime;
import java.time.MonthDay;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;

@Data
@ToString(exclude = {"symbolRankMap", "timeZone", "holidayMaster", "bellService"})
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Venue {
    private final Map<String, Rank> symbolRankMap = Maps.newHashMap();

    private Name name;

    private ZoneId timeZone;

    private Country country;

    private Currency currency;

    private Type type;

    private List<String> symbols;

    private HolidayMaster holidayMaster;

    private int avgLatency;

    private BellService bellService;

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof Venue)) {
            return false;
        }
        if (this == other) {
            return true;
        }
        Venue otherVenue = (Venue) other;
        return this.getName() == otherVenue.getName();
    }


    public boolean isAvailable() {
        return !holidayMaster.isHoliday(MonthDay.now(), this) && bellService.withinBell(LocalDateTime.now().atZone(timeZone));

    }

    public void setRanking(String symbol, Rank rank) {
        symbolRankMap.put(symbol, rank);
    }

    public enum Type {LIT, DARK}

    @AllArgsConstructor
    @Data
    public static class Bell {
        private int hours;
        private int minutes;
    }

    @AllArgsConstructor
    @Data
    public static class BellService {
        private Bell open;
        private Bell close;

        public boolean withinBell(ZonedDateTime zdt) {
            return beforeOpeningBell(zdt) ||
                    afterClosingBell(zdt);
        }

        private boolean afterClosingBell(ZonedDateTime zdt) {
            return zdt.getHour() > close.hours && zdt.getMinute() > close.minutes;
        }

        private boolean beforeOpeningBell(ZonedDateTime zdt) {
            return zdt.getHour() < close.hours && zdt.getMinute() < close.minutes;
        }
    }
}
