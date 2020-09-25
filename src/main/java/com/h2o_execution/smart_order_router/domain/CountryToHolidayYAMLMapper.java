package com.h2o_execution.smart_order_router.domain;


import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.h2o_execution.smart_order_router.core.Country;
import org.springframework.core.io.Resource;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStream;
import java.time.MonthDay;
import java.util.*;

public class CountryToHolidayYAMLMapper implements CountryToHolidayMapper {
    private final Resource resc;

    public CountryToHolidayYAMLMapper(Resource resc) {
        this.resc = resc;
    }

    @Override
    public Optional<Map<Country, Set<MonthDay>>> getMapping() {
        try {
            InputStream inputStream = resc.getInputStream();
            Map<String, List<String>> holidayYamlData = new Yaml().load(inputStream);
            return Optional.of(partitionHolidaysByCountry(holidayYamlData));
        } catch (IOException e) {
            return Optional.empty();
        }
    }

    private Map<Country, Set<MonthDay>> partitionHolidaysByCountry(Map<String, List<String>> holidayYamlData) {
        Map<Country, Set<MonthDay>> holidaysByCountry = Maps.newEnumMap(Country.class);
        for (Country country : Country.values()) {
            Set<MonthDay> holidays = getAllHolidaysInMonthDayFormat(holidayYamlData, country);
            holidaysByCountry.put(country, holidays);
        }
        return holidaysByCountry;
    }

    private Set<MonthDay> getAllHolidaysInMonthDayFormat(Map<String, List<String>> holidayYamlData, Country country) {
        List<String> countryHolidays = holidayYamlData.get(country.toString());
        return getAllHolidaysInMonthDayFormat(countryHolidays);
    }

    private Set<MonthDay> getAllHolidaysInMonthDayFormat(List<String> countryHolidays) {
        Set<MonthDay> holidays = Sets.newHashSet();
        for (String holiday : countryHolidays) {
            holidays.add(parseHoliday(holiday));
        }
        return holidays;
    }

    private MonthDay parseHoliday(String holiday) {
        String[] monthDayStr = holiday.split("/");
        int month = Integer.parseInt(monthDayStr[0]);
        int day = Integer.parseInt(monthDayStr[1]);
        return MonthDay.of(month, day);
    }

}
