package com.h2o_execution.smart_order_router.domain;

import com.h2o_execution.smart_order_router.core.Country;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.time.MonthDay;
import java.util.Map;
import java.util.Set;

@Slf4j
class HolidayMasterTest
{

    @Test
    public void isHolidayTest() throws IOException
    {
        HolidayMaster holidayMaster = new HolidayMaster();
        Map<MonthDay, Set<Country>> holidays = holidayMaster.getHolidays();
        log.info(holidays.toString());
    }
}