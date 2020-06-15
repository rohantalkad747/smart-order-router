package com.h2o_execution.smart_order_router.domain;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.h2o_execution.smart_order_router.core.Country;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.time.MonthDay;
import java.util.*;

@Service
public class HolidayMaster
{
    Resource resourceFile;

    @Getter
    private Map<MonthDay, Set<Country>> holidays;
    
    public HolidayMaster() throws IOException
    {
        this.holidays = new HashMap<>();
        parseCountryHolidays();
    }

    private void parseCountryHolidays() throws IOException
    {
        ResourceLoader resourceLoader = new DefaultResourceLoader();
        Resource resource = resourceLoader.getResource("classpath:holidays.yaml");
        InputStream inputStream = resource.getInputStream();
        Yaml yaml = new Yaml();
        Map<String, List<String>> holidayYamlData = yaml.load(inputStream);
        for ( Country country : Country.values() )
        {
            List<String> countryHolidays = holidayYamlData.get(country.toString());
            for ( String holiday : countryHolidays )
            {
                String[] monthDayStr = holiday.split("/");
                int month = Integer.parseInt(monthDayStr[0]);
                int day = Integer.parseInt(monthDayStr[1]);
                MonthDay md = MonthDay.of(month, day);
                holidays.computeIfAbsent(md, k -> new HashSet<>()).add(country);
            }
        }
    }

    public boolean isHoliday(Venue venue)
    {
        MonthDay monthDay = MonthDay.now();
        Set<Country> countries = holidays.get(monthDay);
        Country country = venue.getCountry();
        return countries != null && countries.contains(country);
    }
}
