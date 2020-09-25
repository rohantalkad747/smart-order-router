package com.h2o_execution.smart_order_router.core;

import com.h2o_execution.smart_order_router.domain.CountryToHolidayMapper;
import com.h2o_execution.smart_order_router.domain.CountryToHolidayYAMLMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

import java.time.MonthDay;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;

class CountryToHolidayMapperTest {
    CountryToHolidayMapper countryToHolidayMapper;

    @BeforeEach
    void loadYAML() {
        ResourceLoader resourceLoader = new DefaultResourceLoader();
        Resource resource = resourceLoader.getResource("classpath:holidays.yaml");
        countryToHolidayMapper = new CountryToHolidayYAMLMapper(resource);
    }

    @Test
    void WHEN_askedForCanadianToHolidayMapping_shouldReturnMapOfHolidays() {
        // Given
        Optional<Map<Country, Set<MonthDay>>> maybeCountryToHolidayMapping = countryToHolidayMapper.getMapping();

        maybeCountryToHolidayMapping.ifPresentOrElse(countryToHolidayMapping -> {

            // When
            Set<MonthDay> canadianHolidays = countryToHolidayMapping.get(Country.CAN);

            // Then
            MonthDay christmas = MonthDay.of(12, 25);
            MonthDay thanksgiving = MonthDay.of(10, 12);
            assertThat(canadianHolidays, containsInAnyOrder(christmas, thanksgiving));
            },

        RuntimeException::new);
    }
}
