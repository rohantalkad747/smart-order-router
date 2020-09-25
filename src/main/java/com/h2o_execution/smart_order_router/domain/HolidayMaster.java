package com.h2o_execution.smart_order_router.domain;

import com.h2o_execution.smart_order_router.core.Country;
import lombok.Getter;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.MonthDay;
import java.util.*;

/**
 * Responsible for determining whether input dates are holidays for the given country.
 */
@Service
public class HolidayMaster {

    @Getter
    private final Map<Country, Set<MonthDay>> holidaysByCountry;

    public HolidayMaster(CountryToHolidayMapper countryToHolidayMapper) {
        holidaysByCountry = countryToHolidayMapper.getMapping().orElseThrow(RuntimeException::new);
    }

    public static HolidayMaster newInstanceFromYamlResource(String path) {
        ResourceLoader resourceLoader = new DefaultResourceLoader();
        Resource resource = resourceLoader.getResource(path);
        CountryToHolidayYAMLMapper countryToHolidayYAMLMapper = new CountryToHolidayYAMLMapper(resource);
        return new HolidayMaster(countryToHolidayYAMLMapper);
    }

    /**
     * @return true if {@code monthDayToTest} is a holiday in the country of the given {@code venue}.
     */
    public boolean isHoliday(MonthDay monthDayToTest, Venue venue) {
        Country country = venue.getCountry();
        Set<MonthDay> holidays = holidaysByCountry.get(country);
        return holidays.contains(monthDayToTest);
    }
}
